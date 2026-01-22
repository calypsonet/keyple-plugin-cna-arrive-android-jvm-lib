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
 * This enumeration provides constants that represent the specific ISO/IEC 14443-4 standards,
 * indicating the type of supported communication between the card and the reader.
 *
 * @since 3.0.0
 */
enum class ArriveContactlessProtocols(internal val techValue: String) {

  /**
   * Represents the ISO/IEC 14443-4 communication protocol with combined support for Type A and Type
   * B cards.
   *
   * @since 3.0.0
   */
  ISO_14443_4_AB("ALL"),

  /**
   * Represents the ISO/IEC 14443-4 communication protocol for Type A cards.
   *
   * @since 3.0.0
   */
  ISO_14443_4_A("A"),

  /**
   * Represents the ISO/IEC 14443-4 communication protocol for Type B cards.
   *
   * @since 3.0.0
   */
  ISO_14443_4_B("B")
}
