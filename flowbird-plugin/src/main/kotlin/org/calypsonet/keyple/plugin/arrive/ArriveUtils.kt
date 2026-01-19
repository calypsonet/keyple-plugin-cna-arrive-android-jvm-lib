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

internal object ArriveUtils {

  internal fun checkNotOnMainThread() {
    check(Looper.myLooper() != Looper.getMainLooper()) {
      "This operation must not be executed on the main UI thread"
    }
  }
}
