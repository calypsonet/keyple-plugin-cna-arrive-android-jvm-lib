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
import android.content.Intent
import com.parkeon.content.BindJoiner
import com.parkeon.periphs.reader.IApduReader
import com.parkeon.services.hunt.HuntInterface
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.eclipse.keyple.core.common.CommonApiProperties
import org.eclipse.keyple.core.plugin.PluginApiProperties
import org.eclipse.keyple.core.plugin.spi.PluginFactorySpi
import org.eclipse.keyple.core.plugin.spi.PluginSpi
import org.eclipse.keyple.core.util.logging.LoggerFactory

/**
 * Adapter class for the Arrive plugin factory.
 *
 * This class provides the implementation of the `ArrivePluginFactory` interface and serves as a
 * bridge for integrating the Arrive plugin into the Keyple framework. It manages the initialization
 * and configuration of necessary components, such as service bindings and intent-based
 * communications, to enable plugin functionality.
 *
 * Responsibilities:
 * - Initializes and binds to required Arrive services using specified intents.
 * - Implements the creation of Arrive-specific plugins through the `getPlugin` method.
 * - Provides version information for both the common API and the plugin API.
 *
 * Key Properties:
 * - `context`: Android context required for initialization and communication with services.
 * - `bindJoiner`: Manages the connection and binding lifecycle to external services.
 * - `huntInterface`: Represents the service interface for card operations.
 * - `iApduReader`: Provides access to the APDU reader for contactless operations.
 *
 * Highlights:
 * - Provides a coroutine-based mechanism to initialize connections using a timeout for graceful
 *   handling of service readiness.
 * - Ensures cleanup and unbinding of services in case of cancellations or errors.
 *
 * @since 3.0.0
 */
internal class ArrivePluginFactoryAdapter(private val context: Context) :
    ArrivePluginFactory, PluginFactorySpi {

  private companion object {
    private val logger = LoggerFactory.getLogger(ArrivePluginFactoryAdapter::class.java)
    private const val HUNT_INTENT_NAME = "cards"
    private const val HUNT_INTENT_TYPE = "hunt/card"
    private const val APDU_INTENT_NAME = "reader_cless"
  }

  private var bindJoiner: BindJoiner? = null
  private var huntInterface: HuntInterface? = null
  private var iApduReader: IApduReader? = null

  override fun getPluginName(): String = ArriveConstants.PLUGIN_NAME

  override fun getPlugin(): PluginSpi =
      ArrivePluginAdapter(context, bindJoiner!!, huntInterface!!, iApduReader!!)

  override fun getCommonApiVersion(): String = CommonApiProperties.VERSION

  override fun getPluginApiVersion(): String = PluginApiProperties.VERSION

  internal suspend fun init(): ArrivePluginFactoryAdapter {
    logger.info("Binding to Arrive services")
    bindJoiner = suspendBindJoinerInitialization(context, initIntents())
    huntInterface = HuntInterface.Stub.asInterface(bindJoiner!!.getService(HUNT_INTENT_NAME))
    iApduReader = IApduReader.Stub.asInterface(bindJoiner!!.getService(APDU_INTENT_NAME))
    logger.info("Arrive services bounded")
    return this
  }

  private fun initIntents(): MutableMap<String, Intent> {
    val intents: MutableMap<String, Intent> = HashMap()
    // HUNT
    val huntIntent = Intent(com.parkeon.content.Intent.ACTION_HUNT)
    huntIntent.type = HUNT_INTENT_TYPE
    intents[HUNT_INTENT_NAME] = huntIntent
    // APDU
    val apduIntent = Intent(com.parkeon.content.Intent.ACTION_APDU_EXCHANGE)
    apduIntent.type = com.parkeon.content.Intent.TYPE_APDU_READER_CONTACTLESS
    intents[APDU_INTENT_NAME] = apduIntent
    return intents
  }

  private suspend fun suspendBindJoinerInitialization(
      context: Context,
      intents: Map<String, Intent>
  ): BindJoiner =
      withTimeout(5_000L) {
        suspendCancellableCoroutine { cont ->
          lateinit var bindJoiner: BindJoiner

          val listener =
              object : BindJoiner.Listener {

                override fun onJoined(initDone: Boolean) {
                  if (!cont.isActive) {
                    logger.error("Arrive services connection established but coroutine cancelled")
                    return
                  }
                  if (initDone) {
                    logger.info("Arrive services connection established")
                    cont.resume(bindJoiner)
                  } else {
                    logger.error("Arrive services connection established but init not done")
                    cont.resumeWithException(
                        IllegalStateException("BindJoiner joined but init not done"))
                  }
                }

                override fun onBindLost(intent: Intent) {
                  if (!cont.isActive) {
                    logger.error("Arrive services connection lost and coroutine cancelled")
                    return
                  }
                  logger.error("Arrive services connection lost")
                  cont.resumeWithException(IllegalStateException("Bind lost for intent $intent"))
                }
              }

          logger.info("Waiting at most 5 seconds for Arrive services connection...")
          bindJoiner = BindJoiner(context, intents, listener)
          bindJoiner.bind()

          cont.invokeOnCancellation {
            logger.error("Unbinding from Arrive services on cancellation")
            bindJoiner.unbind()
          }
        }
      }
}
