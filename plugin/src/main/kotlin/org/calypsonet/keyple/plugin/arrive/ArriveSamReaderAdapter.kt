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

import android.os.RemoteException
import com.parkeon.periphs.reader.IApduReader
import com.parkeon.periphs.reader.IApduReaderExchangeListener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.calypsonet.keyple.plugin.arrive.ArriveUtils.checkNotOnMainThread
import org.eclipse.keyple.core.plugin.CardIOException
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi
import org.eclipse.keyple.core.util.json.JsonUtil
import org.slf4j.LoggerFactory

/**
 * Adapter implementation for the `ArriveSamReader` that bridges functionality between the Arrive
 * plugin and the underlying Secure Application Module (SAM) reader interface.
 *
 * This class integrates with the SAM type, manages communication to the SAM, and provides an
 * implementation of the `ArriveSamReader` and `ReaderSpi` interfaces to support SAM interactions.
 *
 * It includes capabilities for:
 * - Opening and closing the physical communication channel with the SAM.
 * - Checking the presence of the SAM and retrieving its ATR (Answer to Reset) data.
 * - Transmitting APDU (Application Protocol Data Unit) commands to the SAM.
 * - Ensuring operations are executed off the main thread.
 * - Handling timeouts and exceptions during SAM communication.
 *
 * This adapter is designed for internal use within the Arrive plugin infrastructure and relies on
 * the `IApduReader` abstraction for APDU exchanges.
 *
 * @since 3.0.0
 */
internal class ArriveSamReaderAdapter(
    private val sam: ArriveConstants.SAM,
    private val samAtrHex: String,
    private val iApduReader: IApduReader,
) : ArriveSamReader, ReaderSpi {

  private companion object {
    private val logger = LoggerFactory.getLogger(ArriveSamReaderAdapter::class.java)
  }

  private var isPhysicalChannelOpen = false

  override fun getName(): String = sam.readerName

  override fun openPhysicalChannel() {
    isPhysicalChannelOpen = true
    if (logger.isDebugEnabled) {
      logger.debug("SAM channel opened [sam=[{}], atr={}]", sam.readerName, samAtrHex)
    }
  }

  override fun closePhysicalChannel() {
    isPhysicalChannelOpen = false
  }

  override fun isPhysicalChannelOpen(): Boolean {
    return isPhysicalChannelOpen
  }

  override fun checkCardPresence(): Boolean {
    return samAtrHex.isNotEmpty()
  }

  override fun getPowerOnData(): String {
    return samAtrHex
  }

  override fun transmitApdu(apduIn: ByteArray): ByteArray {
    checkNotOnMainThread()
    return try {
      val responses: List<ByteArray> = runBlocking { suspendExchangeWithSam(listOf(apduIn)) }
      val firstResponse = responses.firstOrNull()
      if (firstResponse == null || firstResponse.size < 2) {
        throw IllegalStateException(
            "SAM exchange returned invalid response [data=${JsonUtil.toJson(firstResponse)}]"
        )
      }
      firstResponse
    } catch (_: TimeoutCancellationException) {
      throw CardIOException("SAM exchange timed out")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      throw CardIOException("SAM exchange failed", e)
    }
  }

  private suspend fun suspendExchangeWithSam(commands: List<ByteArray>): List<ByteArray> =
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
                    cont.resumeWithException(RemoteException("SAM exchange returned no response"))
                  }
                }
              }

          iApduReader.exchangeWithSAM(sam.samId, commands, listener)

          cont.invokeOnCancellation {
            // NOP
          }
        }
      }

  override fun isContactless(): Boolean {
    return false
  }

  override fun onUnregister() {
    // NOP
  }
}
