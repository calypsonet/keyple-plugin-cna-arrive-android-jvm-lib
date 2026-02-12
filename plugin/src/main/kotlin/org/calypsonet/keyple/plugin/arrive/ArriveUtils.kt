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

import android.os.Looper

/**
 * Utility object containing helper functions for the Arrive plugin.
 *
 * @since 3.0.0
 */
internal object ArriveUtils {

  /**
   * Ensures that the method invoking this check is not running on the main UI thread.
   *
   * This method is typically used to validate that certain operations which could block the main
   * thread, or create performance bottlenecks, are executed on a background or worker thread.
   *
   * An `IllegalStateException` will be thrown if the current thread is the main UI thread.
   *
   * @throws IllegalStateException if the method is executed on the main UI thread.
   * @since 3.0.0
   */
  internal fun checkNotOnMainThread() {
    check(Looper.myLooper() != Looper.getMainLooper()) {
      "This operation must not be executed on the main UI thread"
    }
  }
}
