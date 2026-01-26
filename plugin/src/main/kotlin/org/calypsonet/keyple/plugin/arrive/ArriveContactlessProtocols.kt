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
 * Defines available contactless communication protocols supported by the Arrive plugin.
 *
 * @since 3.0.0
 */
enum class ArriveContactlessProtocols(internal val transportTypeValue: Int) {

  /**
   * Any ISO 14443-4 compliant card or device (both Type A and Type B).
   *
   * @since 3.0.0
   */
  ISO_14443_4(1),

  /**
   * Calypso cards using Innovatron B Prime protocol.
   *
   * @since 3.0.0
   */
  INNOVATRON_B_PRIME(2),
}
