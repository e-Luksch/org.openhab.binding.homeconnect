/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeconnect.handler;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.homeconnect.internal.client.HomeConnectApiClient;

/**
 * {@link ThingHandler} which implement {@link HomeConnectApiClientListener} will be informed about new {@link HomeConnectApiClient}
 * instances by there parent ({@link HomeConnectBridgeHandler}).
 *
 * @author Jonas Brüstel - Initial contribution
 */
public interface HomeConnectApiClientListener {

    void refreshClient(@NonNull HomeConnectApiClient apiClient);

}
