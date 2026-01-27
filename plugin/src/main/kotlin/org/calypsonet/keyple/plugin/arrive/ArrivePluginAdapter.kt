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
import com.parkeon.content.BindJoiner
import com.parkeon.data.StateHelper
import com.parkeon.periphs.reader.IApduReader
import com.parkeon.services.hunt.HuntInterface
import org.eclipse.keyple.core.plugin.spi.PluginSpi
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi
import org.eclipse.keyple.core.util.logging.LoggerFactory

/**
 * Adapter class for the Arrive plugin implementation.
 *
 * This class serves as a bridge between the Arrive plugin and the Keyple framework, facilitating
 * communication and management of Arrive-specific components such as card readers and SAM (Secure
 * Application Module) devices. It provides the necessary functionality for discovering available
 * readers, handling the plugin lifecycle, and ensuring seamless integration with other framework
 * components.
 *
 * @since 3.0.0
 */
internal class ArrivePluginAdapter(
    private val context: Context,
    private val bindJoiner: BindJoiner,
    private val huntInterface: HuntInterface,
    private val iApduReader: IApduReader,
) : ArrivePlugin, PluginSpi {

  private companion object {
    private val logger = LoggerFactory.getLogger(ArrivePluginAdapter::class.java)
  }

  override fun getName(): String = ArriveConstants.PLUGIN_NAME

  override fun searchAvailableReaders(): Set<ReaderSpi> {
    val stateHelper = StateHelper(context)
    return buildSet {
      add(ArriveCardReaderAdapter(context, huntInterface, iApduReader))
      ArriveConstants.SAM.values().forEach { sam ->
        add(
            ArriveSamReaderAdapter(sam, stateHelper.getAsString(sam.systemStateVarAtr), iApduReader)
        )
      }
    }
  }

  override fun onUnregister() {
    try {
      bindJoiner.unbind()
    } catch (ex: Exception) {
      logger.warn("Failed to unbind properly from Arrive services [reason={}]", ex.message)
    }
  }
}
