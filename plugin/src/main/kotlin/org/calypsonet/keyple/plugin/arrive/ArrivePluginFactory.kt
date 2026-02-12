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

import org.eclipse.keyple.core.common.KeyplePluginExtensionFactory

/**
 * Extends the `KeyplePluginExtensionFactory` interface, which must be registered with the Keyple
 * smart card service to communicate with Arrive Android-based terminals.
 *
 * Use [ArrivePluginFactoryProvider] to retrieve an instance of it.
 *
 * @since 3.0.0
 */
interface ArrivePluginFactory : KeyplePluginExtensionFactory
