/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.plugin.flowbird.reader

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import com.parkeon.content.BindJoiner
import com.parkeon.data.StateHelper
import com.parkeon.periphs.reader.IApduReader
import com.parkeon.periphs.reader.IApduReaderExchangeListener
import java.lang.ref.WeakReference
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.Channel
import org.calypsonet.keyple.plugin.flowbird.Constants.HUNTER_NAME
import org.calypsonet.keyple.plugin.flowbird.Constants.READER_CLESS
import org.calypsonet.keyple.plugin.flowbird.contact.SamSlot
import org.calypsonet.keyple.plugin.flowbird.utils.suspendCoroutineWithTimeout
import org.eclipse.keyple.core.plugin.ReaderIOException
import org.eclipse.keyple.core.util.HexUtil
import timber.log.Timber

internal class FlowbirdReader {

  var readerInstance: IApduReader? = null

  /**
   * Send data to the presented media (phone, card)
   *
   * @param command The command to send
   */
  fun sendCommandToCard(
      command: ByteArray,
      cardId: Long,
      channelResponseApdu: Channel<ByteArray?>?
  ) {
    val commands = mutableListOf(command)
    try {
      Timber.i("FLOW - Exchanging data...\nSEND --> [${HexUtil.toHex(command)}]")
      readerInstance?.exchangeWithCard(
          cardId,
          commands,
          object : IApduReaderExchangeListener.Stub() {
            @Throws(RemoteException::class)
            override fun onExchangeDone(_id: Long, result: Boolean, responses: List<*>) {
              Timber.d(
                  String.format(
                      "Exchange result : %s [Number of responses: %s]", result, responses.size))

              channelResponseApdu?.let {
                if (responses.isNotEmpty()) {
                  it.trySend(responses[0] as ByteArray?).isSuccess
                } else {
                  it.trySend(ByteArray(0)).isSuccess
                }
              }
            }
          })
    } catch (e: RemoteException) {
      Timber.e(e, "Failed to exchange data with NFC.")
    }
  }

  /**
   * Send data to the presented media (phone, card)
   *
   * @param command The command to send
   */
  @Suppress("EXPERIMENTAL_API_USAGE")
  fun sendCommandToSam(
      command: ByteArray,
      samSlot: SamSlot,
      channelResponseApdu: Channel<ByteArray?>?
  ) {
    val commands = mutableListOf(command)
    try {
      Timber.i("FLOW - samSlot : $samSlot - [samId : ${samSlot.slotId.toLong()}]")
      Timber.i("FLOW - Exchanging data...\nSEND --> [${HexUtil.toHex(command)}]")

      readerInstance?.exchangeWithSAM(
          samSlot.slotId.toLong(),
          commands,
          object : IApduReaderExchangeListener.Stub() {
            @Throws(RemoteException::class)
            override fun onExchangeDone(_id: Long, result: Boolean, responses: List<*>) {
              Timber.d(
                  String.format(
                      "Exchange result : %s [Number of responses: %s]", result, responses.size))

              channelResponseApdu?.let {
                if (responses.isNotEmpty()) {
                  it.trySend(responses[0] as ByteArray?).isSuccess
                } else {
                  it.trySend(ByteArray(0)).isSuccess
                }
              }
            }
          })
    } catch (e: RemoteException) {
      Timber.e(e, "Failed to exchange data with NFC.")
    }
  }

  fun getService(name: String?): IBinder? {
    return joiner?.getService(name)
  }

  companion object {

    private const val INIT_TIMEOUT = 2000L

    private const val SAM_1_ATR_KEY = "/contactless/sam1/atr"
    private const val SAM_2_ATR_KEY = "/contactless/sam2/atr"
    private const val SAM_3_ATR_KEY = "/contactless/sam3/atr"
    private const val SAM_4_ATR_KEY = "/contactless/sam4/atr"

    private const val LED_PATTERNS_TYPE_ALL = "all/all"

    @Suppress("EXPERIMENTAL_API_USAGE") val atrContainer = HashMap<SamSlot, ByteArray?>()

    private var joiner: BindJoiner? = null

    private val isInitied = AtomicBoolean(false)
    private lateinit var uniqueInstance: WeakReference<FlowbirdReader?>

    /** Get Reader instance */
    @Throws(ReaderIOException::class)
    fun getInstance(): FlowbirdReader {
      Timber.d("Get Instance")
      if (!isInitied.get() || uniqueInstance.get() == null) {
        throw ReaderIOException("Flowbird Reader not initiated")
      }
      return uniqueInstance.get()!!
    }

    /** Reset the instance */
    fun clearInstance() {
      Timber.d("Clear Flowbird Reader instance")
      joiner!!.unbind()
      getInstance().let {
        it.readerInstance = null
        uniqueInstance = WeakReference(null)
        isInitied.set(false)
      }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    suspend fun initReader(context: Context): Boolean {
      Timber.d("Init Flowbird Reader instance")

      var atrString = StateHelper(context).getAsString(SAM_1_ATR_KEY)
      atrContainer[SamSlot.ONE] = HexUtil.toByteArray(atrString)

      atrString = StateHelper(context).getAsString(SAM_2_ATR_KEY)
      atrContainer[SamSlot.TWO] = HexUtil.toByteArray(atrString)

      atrString = StateHelper(context).getAsString(SAM_3_ATR_KEY)
      atrContainer[SamSlot.THREE] = HexUtil.toByteArray(atrString)

      atrString = StateHelper(context).getAsString(SAM_4_ATR_KEY)
      atrContainer[SamSlot.FOUR] = HexUtil.toByteArray(atrString)

      val services = getServices()

      val isInitDone: Boolean? =
          suspendCoroutineWithTimeout(INIT_TIMEOUT) { continuation ->
            joiner =
                BindJoiner(
                    context,
                    services,
                    object : BindJoiner.Listener {
                      override fun onJoined(initDone: Boolean) {
                        Timber.i("Got all services.")
                        onCreateDone(initDone)

                        continuation.resume(true)
                      }

                      override fun onBindLost(intent: Intent) {
                        continuation.resume(false)
                      }
                    })

            joiner!!.bind()
          }
      Timber.d("Powered on : $isInitDone")
      return isInitDone ?: false
    }

    private fun getServices(): Map<String, Intent> {
      // Get needed services
      val services: MutableMap<String, Intent> = HashMap()

      val ledPatterns = Intent(com.parkeon.content.Intent.ACTION_LEDS_PATTERN)
      ledPatterns.type = LED_PATTERNS_TYPE_ALL
      services["led_patterns"] = ledPatterns
      val ledIntent = Intent(com.parkeon.content.Intent.ACTION_LEDS)
      ledIntent.type = LED_PATTERNS_TYPE_ALL
      services["leds"] = ledIntent

      val authentication = Intent(com.parkeon.content.Intent.ACTION_AUTHENTICATION_SERVICE)
      authentication.type = "agent/maintenance"
      services["authentication"] = authentication
      services["sound"] = Intent(com.parkeon.content.Intent.ACTION_SOUND_SERVICE)

      val intentReader = Intent("com.parkeon.services.card.apdu")
      services[READER_CLESS] = intentReader
      val cardIntent = Intent(com.parkeon.content.Intent.ACTION_HUNT)
      cardIntent.type = "hunt/card"
      services[HUNTER_NAME] = cardIntent

      return services
    }

    private fun onCreateDone(initDone: Boolean) {
      val reader = IApduReader.Stub.asInterface(joiner!!.getService(READER_CLESS))

      uniqueInstance = WeakReference(FlowbirdReader())
      uniqueInstance.get()!!.readerInstance = reader

      isInitied.set(true)

      if (initDone) {
        Timber.i("All services bound")
      } else {
        Timber.i("Fail to bind all services")
      }
    }
  }
}
