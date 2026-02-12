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
package org.calypsonet.keyple.example.plugin.arrive

/** Helper class to provide specific elements to handle Calypso cards. */
object CalypsoConstants {

  /** AID: Keyple test kit profile 1, Application 2 */
  const val AID = "315449432E"

  const val REC_SIZE = 29

  const val SFI_ENV_HOLDER = 0x07.toByte()
  const val SFI_EVENT_LOG = 0x08.toByte()
  const val SFI_CONTRACTS_LIST = 0x1E.toByte()
  const val SFI_CONTRACTS = 0x09.toByte()
  const val SFI_ALL_COUNTERS = 0x19.toByte()

  const val REC_1 = 1
  const val REC_2 = 2
  const val REC_3 = 3
  const val REC_4 = 4
}
