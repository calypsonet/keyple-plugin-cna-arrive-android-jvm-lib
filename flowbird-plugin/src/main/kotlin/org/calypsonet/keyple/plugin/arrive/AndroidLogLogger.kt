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

import android.util.Log
import org.calypsonet.keyple.plugin.arrive.spi.Logger

internal object AndroidLogLogger : Logger {

  override fun debug(tag: String, message: String) {
    Log.d(tag, message)
  }

  override fun info(tag: String, message: String) {
    Log.i(tag, message)
  }

  override fun warn(tag: String, message: String, throwable: Throwable?) {
    Log.w(tag, message, throwable)
  }

  override fun error(tag: String, message: String, throwable: Throwable?) {
    Log.e(tag, message, throwable)
  }
}
