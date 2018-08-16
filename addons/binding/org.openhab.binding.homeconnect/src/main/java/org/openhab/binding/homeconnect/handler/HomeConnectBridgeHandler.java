/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeconnect.handler;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.homeconnect.internal.client.HomeConnectApiClient;
import org.openhab.binding.homeconnect.internal.client.exception.CommunicationException;
import org.openhab.binding.homeconnect.internal.client.exception.ConfigurationException;
import org.openhab.binding.homeconnect.internal.configuration.ApiBridgeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomeConnectBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Jonas Brüstel - Initial contribution
 */
@NonNullByDefault
public class HomeConnectBridgeHandler extends BaseBridgeHandler {

    private static final int REINITIALIZATION_LONG_DELAY = 120;
    private static final int REINITIALIZATION_MEDIUM_DELAY = 30;
    private static final int REINITIALIZATION_SHORT_DELAY = 5;

    private final Logger logger = LoggerFactory.getLogger(HomeConnectBridgeHandler.class);

    @Nullable
    private HomeConnectApiClient apiClient;

    @Nullable
    private ApiBridgeConfiguration config;

    @Nullable
    private ScheduledFuture<?> reinitializationFuture;

    public HomeConnectBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // not used for bridge
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        if (logger.isDebugEnabled()) {
            if (apiClient != null) {
                logger.debug("Updating Home Connect bridge handler");
            } else {
                logger.debug("Initializing Home Connect bridge handler");
            }
        }

        if (apiClient != null) {
            // remove old api client
            apiClient.dispose();
        }

        config = getConfigAs(ApiBridgeConfiguration.class);

        // initialize api client
        apiClient = new HomeConnectApiClient(config.getClientId(), config.getClientSecret(), config.getRefreshToken(),
                config.isSimulator());

        try {
            if (apiClient.getHomeAppliances() != null) {
                updateStatus(ThingStatus.ONLINE);

                // update API clients of bridge children
                logger.debug("Refresh client handlers.");
                List<Thing> children = getThing().getThings();
                for (Thing thing : children) {
                    ThingHandler childHandler = thing.getHandler();
                    if (childHandler instanceof HomeConnectApiClientListener && apiClient != null) {
                        ((HomeConnectApiClientListener) childHandler).refreshClient(apiClient);
                    }
                }

            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Token seems to be invalid!");
                scheduleReinitialize(REINITIALIZATION_LONG_DELAY);
            }
        } catch (ConfigurationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            scheduleReinitialize(REINITIALIZATION_LONG_DELAY);
        } catch (CommunicationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            scheduleReinitialize(REINITIALIZATION_MEDIUM_DELAY);
        }
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        if (childHandler instanceof HomeConnectApiClientListener && apiClient != null) {
            ((HomeConnectApiClientListener) childHandler).refreshClient(apiClient);
        }
    }

    @SuppressWarnings("null")
    @Override
    public void dispose() {
        if (reinitializationFuture != null && !reinitializationFuture.isDone()) {
            reinitializationFuture.cancel(true);
        }
    }

    public @Nullable HomeConnectApiClient getApiClient() {
        return apiClient;
    }

    @SuppressWarnings("null")
    private synchronized void scheduleReinitialize(int seconds) {
        if (reinitializationFuture != null && !reinitializationFuture.isDone()) {
            logger.debug("Reinitialization is already scheduled. Starting in {} seconds.",
                    reinitializationFuture.getDelay(TimeUnit.SECONDS));
        } else {
            reinitializationFuture = scheduler.schedule(() -> {

                scheduler.schedule(() -> initialize(), REINITIALIZATION_SHORT_DELAY, TimeUnit.SECONDS);

            }, seconds, TimeUnit.SECONDS);
        }
    }

}
