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
 * Definition of all supported contactless protocols.
 *
 * @since 3.0.0
 */
enum class ArriveContactlessProtocols(private val techValue: String) {

  /**
   * ISO 14443-4 (A, B)
   *
   * @since 3.0.0
   */
  ISO_14443_4_AB("ALL"),

  /**
   * ISO 14443-4 (A)
   *
   * @since 3.0.0
   */
  ISO_14443_4_A("A"),

  /**
   * ISO 14443-4 (B)
   *
   * @since 3.0.0
   */
  ISO_14443_4_B("B");

  internal fun getTechValue(): String = techValue
}
