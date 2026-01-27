/* **************************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.plugin.arrive

import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import com.parkeon.data.ConfigurationHelper
import com.parkeon.periphs.reader.IApduReader
import com.parkeon.periphs.reader.IApduReaderExchangeListener
import com.parkeon.services.hunt.HuntConstants
import com.parkeon.services.hunt.HuntEventListener
import com.parkeon.services.hunt.HuntInterface
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.calypsonet.keyple.plugin.arrive.ArriveUtils.checkNotOnMainThread
import org.eclipse.keyple.core.plugin.CardIOException
import org.eclipse.keyple.core.plugin.CardInsertionWaiterAsynchronousApi
import org.eclipse.keyple.core.plugin.CardRemovalWaiterAsynchronousApi
import org.eclipse.keyple.core.plugin.ReaderIOException
import org.eclipse.keyple.core.plugin.spi.reader.ConfigurableReaderSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.CardInsertionWaiterAsynchronousSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.CardRemovalWaiterAsynchronousSpi
import org.eclipse.keyple.core.util.json.JsonUtil
import org.eclipse.keyple.core.util.logging.LoggerFactory

/**
 * Adapter class that provides the functionality required to interact with the Arrive card reader.
 *
 * This class acts as a bridge between the Arrive-specific card reader interface and the underlying
 * device APIs. It manages configurations, handles contactless protocol activation, facilitates APDU
 * communication with the card, and provides detection management for card insertion and removal
 * events.
 *
 * Responsibilities include:
 * - Managing the communication sessions via physical channels and ensuring they are properly opened
 *   and closed.
 * - Supporting the activation and deactivation of specific contactless protocols.
 * - Facilitating card insertion and removal events using asynchronous callbacks.
 * - Enabling APDU commands to be transmitted to the detected card and handling responses
 *   accordingly.
 * - Managing the state of card detection to ensure proper operation.
 *
 * This class integrates with low-level device services and API components, including:
 * - `HuntInterface` for device detection events.
 * - `IApduReader` for transmitting APDU instructions.
 *
 * It registers a custom event listener `MyHuntEventListener` to handle events such as card
 * detection, removal, and errors. The configuration for the card reader is managed using a
 * `ConfigurationHelper`. The adapter ensures that all operations such as exchanges with the card
 * are performed off the main UI thread to avoid blocking.
 *
 * Exceptions are thrown or propagated in cases of communication failure, timeouts, or invalid input
 * to maintain reliable operation.
 *
 * @since 3.0.0
 */
internal class ArriveCardReaderAdapter(
    context: Context,
    private val huntInterface: HuntInterface,
    private val iApduReader: IApduReader,
) :
    ArriveCardReader,
    ConfigurableReaderSpi,
    ObservableReaderSpi,
    CardInsertionWaiterAsynchronousSpi,
    CardRemovalWaiterAsynchronousSpi {

  private companion object {
    private val logger = LoggerFactory.getLogger(ArriveCardReaderAdapter::class.java)
    private const val CONFIG_PROTOCOL_A_KEY = "/contactless/hunt/A"
    private const val CONFIG_PROTOCOL_B_KEY = "/contactless/hunt/B"
    private const val CONFIG_PROTOCOL_BP_KEY = "/contactless/hunt/BP"
    private const val TAG_CARD_ID = "id"
  }

  private lateinit var cardInsertionWaiterAsynchronousApi: CardInsertionWaiterAsynchronousApi
  private lateinit var cardRemovalWaiterAsynchronousApi: CardRemovalWaiterAsynchronousApi

  private var configurationHelper: ConfigurationHelper

  private var currentCardProtocol: Int? = null
  private var currentCardId: Long? = null
  private var currentCardAtr: String? = null

  private var isPhysicalChannelOpen = false
  private var isCardDetectionStarted: Boolean = false
  private var huntEventListener = MyHuntEventListener()

  init {
    huntInterface.addEventListener(huntEventListener)
    configurationHelper = ConfigurationHelper(context)
  }

  override fun getName(): String = ArriveConstants.CARD_READER_NAME

  override fun openPhysicalChannel() {
    isPhysicalChannelOpen = true
  }

  override fun closePhysicalChannel() {
    isPhysicalChannelOpen = false
  }

  override fun isPhysicalChannelOpen(): Boolean {
    return isPhysicalChannelOpen
  }

  override fun checkCardPresence(): Boolean {
    return currentCardAtr != null
  }

  override fun getPowerOnData(): String {
    return currentCardAtr!!
  }

  override fun transmitApdu(apduIn: ByteArray): ByteArray {
    checkNotOnMainThread()
    return try {
      val responses: List<ByteArray> = runBlocking { suspendExchangeWithCard(listOf(apduIn)) }
      val firstResponse = responses.firstOrNull()
      if (firstResponse == null || firstResponse.size < 2) {
        throw IllegalStateException(
            "Card exchange returned invalid response [data=${
          JsonUtil.toJson(
            firstResponse
          )
        }]"
        )
      }
      firstResponse
    } catch (_: TimeoutCancellationException) {
      throw CardIOException("Card exchange timed out")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      throw CardIOException("Card exchange failed", e)
    }
  }

  private suspend fun suspendExchangeWithCard(commands: List<ByteArray>): List<ByteArray> =
      withTimeout(5_000L) {
        suspendCancellableCoroutine { cont ->
          val listener =
              object : IApduReaderExchangeListener.Stub() {

                override fun onExchangeDone(id: Long, result: Boolean, responses: List<*>?) {
                  if (!cont.isActive) {
                    return
                  }
                  if (result) {
                    @Suppress("UNCHECKED_CAST") cont.resume(responses as List<ByteArray>)
                  } else {
                    cont.resumeWithException(RemoteException("Card exchange returned no response"))
                  }
                }
              }

          iApduReader.exchangeWithCard(currentCardId ?: 0, commands, listener)

          cont.invokeOnCancellation {
            // NOP
          }
        }
      }

  override fun isContactless(): Boolean {
    return true
  }

  override fun onUnregister() {
    onStopDetection()
    huntInterface.removeEventListener(huntEventListener)
  }

  override fun isProtocolSupported(readerProtocol: String): Boolean {
    return try {
      ArriveContactlessProtocols.valueOf(readerProtocol)
      true
    } catch (_: IllegalArgumentException) {
      false
    }
  }

  override fun activateProtocol(readerProtocol: String) {
    val protocol = ArriveContactlessProtocols.valueOf(readerProtocol)
    when (protocol) {
      ArriveContactlessProtocols.ISO_14443_4 -> {
        configurationHelper.set(CONFIG_PROTOCOL_A_KEY, 1)
        configurationHelper.set(CONFIG_PROTOCOL_B_KEY, 1)
      }
      ArriveContactlessProtocols.INNOVATRON_B_PRIME -> {
        configurationHelper.set(CONFIG_PROTOCOL_BP_KEY, 1)
      }
    }
  }

  override fun deactivateProtocol(readerProtocol: String) {
    val protocol = ArriveContactlessProtocols.valueOf(readerProtocol)
    when (protocol) {
      ArriveContactlessProtocols.ISO_14443_4 -> {
        configurationHelper.set(CONFIG_PROTOCOL_A_KEY, 0)
        configurationHelper.set(CONFIG_PROTOCOL_B_KEY, 0)
      }
      ArriveContactlessProtocols.INNOVATRON_B_PRIME -> {
        configurationHelper.set(CONFIG_PROTOCOL_BP_KEY, 0)
      }
    }
  }

  override fun isCurrentProtocol(readerProtocol: String): Boolean {
    val protocol = ArriveContactlessProtocols.valueOf(readerProtocol)
    return currentCardProtocol?.let { it == protocol.transportTypeValue } ?: false
  }

  override fun onStartDetection() {
    logger.debug("Starting card detection")
    try {
      huntInterface.startDetection(Bundle())
      logger.debug("Card detection started")
    } catch (e: RemoteException) {
      throw ReaderIOException("Failed to start card detection", e)
    }
    isCardDetectionStarted = true
  }

  override fun onStopDetection() {
    logger.debug("Stopping card detection")
    try {
      if (huntInterface.stopDetection()) {
        logger.debug("Card detection stopped")
      } else {
        logger.debug("Card detection was not started")
      }
    } catch (e: RemoteException) {
      throw ReaderIOException("Failed to stop card detection", e)
    } finally {
      isCardDetectionStarted = false
    }
  }

  override fun setCallback(callback: CardInsertionWaiterAsynchronousApi) {
    cardInsertionWaiterAsynchronousApi = callback
  }

  override fun setCallback(callback: CardRemovalWaiterAsynchronousApi) {
    cardRemovalWaiterAsynchronousApi = callback
  }

  private inner class MyHuntEventListener : HuntEventListener.Stub() {

    override fun getListenerId(): String = name

    override fun onDetected(data: Bundle) {
      logger.info("Card detected")
      currentCardProtocol = data.getInt(HuntConstants.TAG_CARD_TYPE_TRANSPORT)
      currentCardId = data.getLong(TAG_CARD_ID)
      currentCardAtr = data.getString(HuntConstants.TAG_ATR)
      if (isCardDetectionStarted) {
        cardInsertionWaiterAsynchronousApi.onCardInserted()
      }
    }

    override fun onRemoved(data: Bundle) {
      logger.info("Card removed")
      currentCardId = null
      currentCardAtr = null
      cardRemovalWaiterAsynchronousApi.onCardRemoved()
      if (isCardDetectionStarted) {
        onStartDetection()
      }
    }

    override fun onError(data: Bundle) {
      logger.error("Card detection error [errorData={}]", JsonUtil.toJson(data))
      CoroutineScope(Dispatchers.IO).launch {
        delay(500)
        if (isCardDetectionStarted) {
          onStartDetection()
        }
      }
    }
  }
}
