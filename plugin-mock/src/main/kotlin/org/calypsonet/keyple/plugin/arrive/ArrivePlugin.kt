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

import org.eclipse.keyple.core.common.KeyplePluginExtension

/**
 * Extends the `KeyplePluginExtension` interface dedicated to the Arrive plugin.
 *
 * The Arrive plugin is a Keyple plugin extension that integrates with the Keyple framework to
 * support specific functionality related to the Arrive system. This includes managing communication
 * with Arrive card readers and Secure Application Modules (SAMs), and facilitating operations such
 * as the discovery and interaction with these devices.
 *
 * @since 3.0.0
 */
interface ArrivePlugin : KeyplePluginExtension
