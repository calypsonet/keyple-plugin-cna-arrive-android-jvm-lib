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

/**
 * Constants related to the Arrive plugin.
 *
 * @since 3.0.0
 */
object ArriveConstants {

  /**
   * The plugin name as registered to the Keyple smart card service.
   *
   * @since 3.0.0
   */
  const val PLUGIN_NAME = "ArrivePlugin"

  /**
   * The card reader name as provided by the plugin.
   *
   * @since 3.0.0
   */
  const val CARD_READER_NAME = "ArriveCardReader"

  /**
   * Represents the Secure Application Module (SAM) types that are compatible with the Arrive
   * plugin.
   *
   * Each SAM instance is associated with a specific reader name, as provided by the plugin.
   *
   * @property readerName The name of the SAM reader associated with the specific SAM type.
   * @since 3.0.0
   */
  enum class SAM(val readerName: String) {

    SAM_1("ArriveSamReader1"),
    SAM_2("ArriveSamReader2"),
    SAM_3("ArriveSamReader3"),
    SAM_4("ArriveSamReader4");

    internal val samId: Long = ordinal.toLong()
    internal val systemStateVarAtr: String = "/contactless/sam${ordinal + 1}/atr"
  }
}
