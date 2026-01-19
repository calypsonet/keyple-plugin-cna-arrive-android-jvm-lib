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
import com.parkeon.services.hunt.HuntEventListener
import com.parkeon.services.hunt.HuntInterface
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.calypsonet.keyple.plugin.arrive.ArriveConstants.TAG
import org.calypsonet.keyple.plugin.arrive.ArriveUtils.checkNotOnMainThread
import org.calypsonet.keyple.plugin.arrive.spi.Logger
import org.eclipse.keyple.core.plugin.CardIOException
import org.eclipse.keyple.core.plugin.CardInsertionWaiterAsynchronousApi
import org.eclipse.keyple.core.plugin.CardRemovalWaiterAsynchronousApi
import org.eclipse.keyple.core.plugin.ReaderIOException
import org.eclipse.keyple.core.plugin.spi.reader.ConfigurableReaderSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.CardInsertionWaiterAsynchronousSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.CardRemovalWaiterAsynchronousSpi
import org.eclipse.keyple.core.util.json.JsonUtil

internal class ArriveCardReaderAdapter(
    private val context: Context,
    private val logger: Logger,
    private val huntInterface: HuntInterface,
    private val iApduReader: IApduReader
) :
    ArriveCardReader,
    ConfigurableReaderSpi,
    ObservableReaderSpi,
    CardInsertionWaiterAsynchronousSpi,
    CardRemovalWaiterAsynchronousSpi {

  private companion object {
    private const val CONFIG_CURRENT_PROTOCOL_KEY = "/contactless/hunt/pollscript/modes/current"
    private const val CONFIG_ACTIVE_PROTOCOL_KEY = "/contactless/hunt/pollscript/modes/active"
  }

  private lateinit var cardInsertionWaiterAsynchronousApi: CardInsertionWaiterAsynchronousApi
  private lateinit var cardRemovalWaiterAsynchronousApi: CardRemovalWaiterAsynchronousApi

  private var configurationHelper: ConfigurationHelper

  private var currentProtocol: ArriveContactlessProtocols? = null
  private var currentCardId: Long? = null
  private var currentCardAtr: String? = null
  private var isPhysicalChannelOpen = false
  private var myHuntEventListener = MyHuntEventListener()

  init {
    huntInterface.addEventListener(myHuntEventListener)
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
      responses.firstOrNull() ?: throw IllegalStateException("CARD exchange returned no response")
    } catch (_: TimeoutCancellationException) {
      throw CardIOException("CARD exchange timed out")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      throw CardIOException("CARD exchange failed: ${e.message}", e)
    }
  }

  suspend fun suspendExchangeWithCard(commands: List<ByteArray>): List<ByteArray> =
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
                    cont.resumeWithException(RemoteException("CARD exchange returned no response"))
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
    huntInterface.removeEventListener(myHuntEventListener)
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
    currentProtocol = ArriveContactlessProtocols.valueOf(readerProtocol)
    configurationHelper.set(CONFIG_CURRENT_PROTOCOL_KEY, currentProtocol!!.getTechValue())
  }

  override fun deactivateProtocol(readerProtocol: String) {
    // NOP
  }

  override fun isCurrentProtocol(readerProtocol: String): Boolean {
    return currentProtocol?.let { it.name == readerProtocol } ?: false
  }

  override fun onStartDetection() {
    logger.info(TAG, "Starting card detection...")
    try {
      huntInterface.startDetection(Bundle())
    } catch (e: RemoteException) {
      throw ReaderIOException("Failed to start card detection: ${e.message}", e)
    }
  }

  override fun onStopDetection() {
    logger.info(TAG, "Stoping card detection...")
    try {
      if (!huntInterface.stopDetection()) {
        throw ReaderIOException("Failed to stop card detection")
      }
    } catch (e: RemoteException) {
      throw ReaderIOException("Failed to stop card detection: ${e.message}", e)
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
      logger.info(TAG, "Card detected")
      currentCardId = data.getLong("id")
      currentCardAtr = data.getString("atr")
      cardInsertionWaiterAsynchronousApi.onCardInserted()
    }

    override fun onRemoved(data: Bundle) {
      logger.info(TAG, "Card removed")
      currentCardId = null
      currentCardAtr = null
      cardRemovalWaiterAsynchronousApi.onCardRemoved()
    }

    override fun onError(data: Bundle) {
      logger.error(TAG, "Card detection error: ${JsonUtil.toJson(data)}")
    }
  }
}
