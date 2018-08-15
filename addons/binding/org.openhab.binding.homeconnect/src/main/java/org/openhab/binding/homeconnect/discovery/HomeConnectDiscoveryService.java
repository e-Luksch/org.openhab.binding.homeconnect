/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeconnect.discovery;

import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.openhab.binding.homeconnect.handler.HomeConnectBridgeHandler;
import org.openhab.binding.homeconnect.internal.client.HomeConnectApiClient;
import org.openhab.binding.homeconnect.internal.client.exception.ConfigurationException;
import org.openhab.binding.homeconnect.internal.client.model.HomeAppliance;
import org.openhab.binding.homeconnect.internal.configuration.ApiBridgeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomeConnectDiscoveryService} is responsible for discovering new devices.
 *
 * @author Jonas Brüstel - Initial contribution
 */
public class HomeConnectDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(HomeConnectDiscoveryService.class);
    private static final int SEARCH_TIME = 50;

    @NonNullByDefault
    private HomeConnectBridgeHandler bridgeHandler;

    /**
     * Construct an {@link HomeConnectDiscoveryService} with the given {@link BridgeHandler}.
     *
     * @param bridgeHandler
     */
    public HomeConnectDiscoveryService(@NonNull HomeConnectBridgeHandler bridgeHandler) {
        super(SUPPORTED_DEVICE_THING_TYPES_UIDS, SEARCH_TIME, true);
        this.bridgeHandler = bridgeHandler;
    }

    @Override
    protected void startScan() {
        logger.debug("Starting device scan.");

        ApiBridgeConfiguration config = bridgeHandler.getThing().getConfiguration().as(ApiBridgeConfiguration.class);

        // initialize api client
        HomeConnectApiClient apiClient = new HomeConnectApiClient(config.getClientId(), config.getClientSecret(),
                config.getRefreshToken(), config.isSimulator());

        try {
            List<HomeAppliance> appliances = apiClient.getHomeAppliances();
            if (appliances != null) {
                logger.info("Found the following devices {}", appliances);

                // add found devices
                for (HomeAppliance appliance : appliances) {
                    if (alreadyExists(appliance.getHaId())) {
                        logger.debug("[{}] Device already added '{}'.", appliance.getHaId(), appliance.getType());
                    } else if (THING_TYPE_DISHWASHER.getId().equalsIgnoreCase(appliance.getType())) {
                        bridgeHandler.getThing().getThings().forEach(thing -> thing.getProperties().get(HA_ID));

                        Map<String, Object> properties = new HashMap<>();
                        properties.put(HA_ID, appliance.getHaId());
                        String name = appliance.getBrand() + " " + appliance.getName() + " (" + appliance.getHaId()
                                + ")";

                        DiscoveryResult discoveryResult = DiscoveryResultBuilder
                                .create(new ThingUID(THING_TYPE_DISHWASHER.getBindingId(),
                                        THING_TYPE_DISHWASHER.getId(), appliance.getHaId()))
                                .withThingType(THING_TYPE_DISHWASHER).withProperties(properties)
                                .withBridge(bridgeHandler.getThing().getUID()).withLabel(name).build();
                        thingDiscovered(discoveryResult);
                    } else {
                        logger.info("[{}]Ignoring unsupported device type '{}'.", appliance.getHaId(),
                                appliance.getType());
                    }
                }

            }
        } catch (ConfigurationException e) {
            logger.debug("Configuration exception. Stopping scan.", e);
        } catch (Exception e) {
            logger.error("Exception during scan.", e);
        } finally {
            apiClient.dispose();
            logger.debug("Finished device scan.");
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
        removeOlderResults(new Date().getTime());
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }

    /**
     * Check if device is already connected to the bridge.
     *
     * @param haId home appliance id
     * @return
     */
    private boolean alreadyExists(String haId) {
        boolean exists = false;
        List<Thing> children = bridgeHandler.getThing().getThings();
        for (Thing child : children) {
            if (haId.equals(child.getConfiguration().get(HA_ID))) {
                exists = true;
            }
        }
        return exists;
    }

}
