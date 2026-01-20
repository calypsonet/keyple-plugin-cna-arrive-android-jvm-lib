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
import org.eclipse.keyple.core.util.logging.LoggerFactory

internal class ArriveSamReaderAdapter(
    private val sam: ArriveConstants.SAM,
    private val samAtrHex: String,
    private val iApduReader: IApduReader
) : ArriveSamReader, ReaderSpi {

  private companion object {
    private val logger = LoggerFactory.getLogger(ArriveSamReaderAdapter::class.java)
  }

  private var isPhysicalChannelOpen = false

  override fun getName(): String = sam.readerName

  override fun openPhysicalChannel() {
    logger.info("Opening SAM channel: [ATR=${samAtrHex}]")
    isPhysicalChannelOpen = true
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
      responses.firstOrNull() ?: throw IllegalStateException("SAM exchange returned no response")
    } catch (_: TimeoutCancellationException) {
      throw CardIOException("SAM exchange timed out")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      throw CardIOException("SAM exchange failed: ${e.message}", e)
    }
  }

  suspend fun suspendExchangeWithSam(commands: List<ByteArray>): List<ByteArray> =
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
