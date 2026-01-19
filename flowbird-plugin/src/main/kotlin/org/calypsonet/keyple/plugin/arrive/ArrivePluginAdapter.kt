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
import org.calypsonet.keyple.plugin.arrive.ArriveConstants.TAG
import org.calypsonet.keyple.plugin.arrive.spi.Logger
import org.eclipse.keyple.core.plugin.spi.PluginSpi
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi

internal class ArrivePluginAdapter(
    private val context: Context,
    private val logger: Logger,
    private val bindJoiner: BindJoiner,
    private val huntInterface: HuntInterface,
    private val iApduReader: IApduReader
) : ArrivePlugin, PluginSpi {

  override fun getName(): String = ArriveConstants.PLUGIN_NAME

  override fun searchAvailableReaders(): Set<ReaderSpi> {
    val stateHelper = StateHelper(context)
    return buildSet {
      add(ArriveCardReaderAdapter(context, logger, huntInterface, iApduReader))
      ArriveConstants.SAM.values().forEach { sam ->
        add(
            ArriveSamReaderAdapter(
                sam, stateHelper.getAsString(sam.systemStateVarAtr), logger, iApduReader))
      }
    }
  }

  override fun onUnregister() {
    try {
      bindJoiner.unbind()
    } catch (ex: Exception) {
      logger.warn(TAG, "Plugin unregistration error: ${ex.message}")
    }
  }
}
