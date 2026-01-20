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
 * Provider of [ArrivePluginFactory] instances.
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
  suspend fun provideFactory(context: Context): ArrivePluginFactory {
    return ArrivePluginFactoryAdapter(context).init()
  }
}
