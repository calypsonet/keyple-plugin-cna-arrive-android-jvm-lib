/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.plugin.flowbird.utils

import android.os.Looper

/**
 * Helper class to execute runnable in background. Thread Useful to call AsyncTask from background
 * (or unknown) thread.
 */
class BackgroundThreadExecutor {

  companion object {
    val isUiThread: Boolean
      get() = Thread.currentThread() === Looper.getMainLooper().thread
  }
}
