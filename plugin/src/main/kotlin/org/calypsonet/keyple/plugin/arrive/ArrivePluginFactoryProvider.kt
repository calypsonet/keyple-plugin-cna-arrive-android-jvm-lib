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

/**
 * Singleton provider for creating instances of [ArrivePluginFactory].
 *
 * This object is responsible for supplying properly initialized instances of the
 * [ArrivePluginFactory] interface. It ensures that the required components and services are bound
 * and configured within the context of the Android application.
 *
 * Responsibilities:
 * - Provides native CountDownLatch-based initialization for connecting to necessary Arrive
 *   services.
 * - Returns fully configured factory instances capable of creating Arrive plugin components.
 *
 * @since 3.0.0
 */
object ArrivePluginFactoryProvider {

  /**
   * Provides an instance of [ArrivePluginFactory] for the given Android context.
   *
   * @param context The Android context to be used for initializing the plugin factory.
   * @return An instance of [ArrivePluginFactory] initialized with the provided context.
   * @since 3.0.0
   */
  fun provideFactory(context: Context): ArrivePluginFactory {
    return ArrivePluginFactoryAdapter(context)
  }
}
