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

import org.eclipse.keyple.core.common.KeypleReaderExtension

/**
 * Extends the `KeypleReaderExtension` interface dedicated to the Arrive card contactless reader.
 *
 * This interface defines the capabilities and behaviors of a card contactless reader within the
 * context of the Arrive system. It extends the capabilities of the generic `KeypleReaderExtension`
 * to provide Arrive-specific functionalities.
 *
 * @since 3.0.0
 */
interface ArriveCardReader : KeypleReaderExtension
