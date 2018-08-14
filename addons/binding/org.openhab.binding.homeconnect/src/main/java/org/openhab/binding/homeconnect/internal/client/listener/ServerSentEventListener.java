/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeconnect.internal.client.listener;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.binding.homeconnect.internal.client.model.Event;

/**
 * {@link ServerSentEventListener} inform about new events from Home Connect SSE interface.
 *
 * @author Jonas Brüstel - Initial contribution
 */
public interface ServerSentEventListener {

    /**
     * Home appliance id of interest
     *
     * @return
     */
    String haId();

    /**
     * Inform listener about new event
     *
     * @param event
     */
    void onEvent(@NonNull Event event);

    /**
     * If SSE client did a reconnect
     */
    void onReconnect();
}
