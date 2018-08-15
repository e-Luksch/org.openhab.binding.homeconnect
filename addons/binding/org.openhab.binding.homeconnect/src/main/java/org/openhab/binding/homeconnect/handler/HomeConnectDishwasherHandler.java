/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeconnect.handler;

import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.*;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.homeconnect.internal.client.HomeConnectApiClient;
import org.openhab.binding.homeconnect.internal.client.exception.CommunicationException;
import org.openhab.binding.homeconnect.internal.client.exception.ConfigurationException;
import org.openhab.binding.homeconnect.internal.client.listener.ServerSentEventListener;
import org.openhab.binding.homeconnect.internal.client.model.Data;
import org.openhab.binding.homeconnect.internal.client.model.Event;
import org.openhab.binding.homeconnect.internal.client.model.HomeAppliance;
import org.openhab.binding.homeconnect.internal.client.model.Option;
import org.openhab.binding.homeconnect.internal.client.model.Program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomeConnectDishwasherHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Jonas Brüstel - Initial contribution
 */
@NonNullByDefault
public class HomeConnectDishwasherHandler extends BaseThingHandler implements HomeConnectApiClientListener {

    private final Logger logger = LoggerFactory.getLogger(HomeConnectDishwasherHandler.class);

    @Nullable
    private HomeConnectApiClient client;

    @Nullable
    private ServerSentEventListener serverSentEventListener;

    public HomeConnectDishwasherHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        Bridge bridge = getBridge();
        HomeConnectApiClient apiClient = client;
        if (bridge == null) {
            logger.warn("BridgeHandler not found. Cannot handle command without bridge.");
            return;
        }
        if (ThingStatus.OFFLINE.equals(bridge.getStatus())) {
            logger.debug("Bridge is OFFLINE. Ignore command.");
            return;
        }
        if (apiClient == null) {
            logger.warn("No API client available.");
            return;
        }

        if (command instanceof RefreshType) {
            // refresh channel
            updateChannel(channelUID);
        } else if (command instanceof OnOffType && channelUID.getId().equals(CHANNEL_DISHWASHER_POWER_STATE)) {

            try {
                // turn dishwasher on and off
                apiClient.setPowerState(getThingHaId(),
                        OnOffType.ON.equals(command) ? STATE_POWER_ON : STATE_POWER_OFF);
            } catch (ConfigurationException | CommunicationException e) {
                logger.error("API communication problem!", e);
            }
            // update channel state via API (if the device is running you're not allowed to turn off the device)
            updateChannel(channelUID);
        } else {
            logger.debug("Unhandeled command: {} for channel: {}", command, channelUID);
        }

    }

    @Override
    public void initialize() {
        // wait for bridge to be setup first
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void refreshClient(@NonNull HomeConnectApiClient apiClient) {
        client = apiClient;
        serverSentEventListener = new ServerSentEventListener() {

            @Override
            public void onEvent(Event event) {
                logger.debug("[{}] {}", getThingHaId(), event);
                Channel channel = null;

                switch (event.getKey()) {
                    case STATE_POWER:
                        channel = getThing().getChannel(CHANNEL_DISHWASHER_POWER_STATE);
                        break;
                    case STATE_DOOR:
                        channel = getThing().getChannel(CHANNEL_DISHWASHER_DOOR_STATE);
                        break;
                    case STATE_OPERATION:
                        channel = getThing().getChannel(CHANNEL_DISHWASHER_OPERATION_STATE);
                        break;
                    case STATE_REMOTE_CONTROL:
                        channel = getThing().getChannel(CHANNEL_DISHWASHER_REMOTE_CONTROL_ACTIVE_STATE);
                        break;
                    case STATE_REMOTE_START:
                        channel = getThing().getChannel(CHANNEL_DISHWASHER_REMOTE_START_ALLOWANCE_STATE);
                        break;
                    case OPTION_PROGRAM_PROGRESS:
                        channel = getThing().getChannel(CHANNEL_DISHWASHER_PROGRAM_PROGRESS_STATE);
                        break;
                    case OPTION_REMAINING_PROGRAM_TIME:
                        channel = getThing().getChannel(CHANNEL_DISHWASHER_REMAINING_PROGRAM_TIME_STATE);
                        break;
                    case STATE_ACTIVE_PROGRAM:
                        channel = getThing().getChannel(CHANNEL_DISHWASHER_ACTIVE_PROGRAM_STATE);
                        break;
                    default:
                        logger.debug("[{}] Ignore event {}", getThingHaId(), event);
                        break;
                }

                if (channel != null) {
                    ChannelUID channelUID = channel.getUID();
                    updateState(channelUID, createState(channelUID, event.getValue()));

                    // if active program change is received --> update progress and remaining channels via API call
                    if (event.getKey().equals(STATE_ACTIVE_PROGRAM)) {
                        Channel progress = getThing().getChannel(CHANNEL_DISHWASHER_PROGRAM_PROGRESS_STATE);
                        if (progress != null) {
                            updateChannel(progress.getUID());
                        }
                        Channel remaining = getThing().getChannel(CHANNEL_DISHWASHER_REMAINING_PROGRAM_TIME_STATE);
                        if (remaining != null) {
                            updateChannel(remaining.getUID());
                        }
                    }
                }
            }

            @Override
            public String haId() {
                return getThingHaId();
            }

            @Override
            public void onReconnect() {
                // update all channels via API
                updateChannels();
            }
        };
        try {
            apiClient.registerEventListener(serverSentEventListener);
            refreshConnectionStatus();
            if (ThingStatus.ONLINE.equals(getThing().getStatus())) {
                updateChannels();
            }
        } catch (ConfigurationException | CommunicationException e) {
            logger.error("API communication problem!", e);
        }

    }

    @Override
    public void dispose() {
        if (serverSentEventListener != null && client != null) {
            client.unregisterEventListener(serverSentEventListener);
        }
    }

    /**
     * Update all channels via API.
     *
     */
    private void updateChannels() {
        List<Channel> channels = getThing().getChannels();
        for (Channel channel : channels) {
            updateChannel(channel.getUID());
        }
    }

    /**
     * Update Channel values via API.
     *
     * @param channelUID
     */
    private void updateChannel(ChannelUID channelUID) {
        HomeConnectApiClient apiClient = client;

        if (apiClient == null) {
            logger.error("Cannot update channel. No instance of api client found!");
            return;
        }

        try {
            if (channelUID.getId().equals(CHANNEL_DISHWASHER_OPERATION_STATE)) {
                updateState(channelUID, createState(channelUID, apiClient.getOperationState(getThingHaId())));
            } else if (channelUID.getId().equals(CHANNEL_DISHWASHER_DOOR_STATE)) {
                updateState(channelUID, createState(channelUID, apiClient.getDoorState(getThingHaId())));
            } else if (channelUID.getId().equals(CHANNEL_DISHWASHER_POWER_STATE)) {
                updateState(channelUID, createState(channelUID, apiClient.getPowerState(getThingHaId())));
            } else if (channelUID.getId().equals(CHANNEL_DISHWASHER_REMOTE_START_ALLOWANCE_STATE)) {
                updateState(channelUID, createState(channelUID, apiClient.isRemoteControlStartAllowed(getThingHaId())));
            } else if (channelUID.getId().equals(CHANNEL_DISHWASHER_REMOTE_CONTROL_ACTIVE_STATE)) {
                updateState(channelUID, createState(channelUID, apiClient.isRemoteControlActive(getThingHaId())));
            } else if (channelUID.getId().equals(CHANNEL_DISHWASHER_ACTIVE_PROGRAM_STATE)
                    || channelUID.getId().equals(CHANNEL_DISHWASHER_REMAINING_PROGRAM_TIME_STATE)
                    || channelUID.getId().equals(CHANNEL_DISHWASHER_PROGRAM_PROGRESS_STATE)) {
                updateState(channelUID, createState(channelUID, apiClient.getActiveProgram(getThingHaId())));
            }
        } catch (ConfigurationException | CommunicationException e) {
            logger.error("API communication problem!", e);
        }
    }

    /**
     * Create channel specific {@link State} based on {@link Data} model value.
     *
     * @param channelUID
     * @param value      model
     * @return {@link UnDefType} NULL or channel specific state
     */
    private State createState(ChannelUID channelUID, @Nullable Data value) {
        if (value == null) {
            return UnDefType.NULL;
        }
        return createState(channelUID, value.getValue());
    }

    /**
     * Create channel specific {@link State} based on boolean value.
     *
     * @param channelUID
     * @param value      boolean value
     * @return {@link UnDefType} NULL or channel specific state
     */
    private State createState(ChannelUID channelUID, boolean value) {
        return createState(channelUID, String.valueOf(value));
    }

    /**
     * Create channel specific {@link State} based on {@link Program} model value.
     *
     * @param channelUID
     * @param program    model
     * @return {@link UnDefType} NULL or channel specific state
     */
    private State createState(ChannelUID channelUID, @Nullable Program program) {
        if (program != null) {
            if (channelUID.getId().equals(CHANNEL_DISHWASHER_ACTIVE_PROGRAM_STATE)) {
                return createState(channelUID, program.getKey());
            } else if (channelUID.getId().equals(CHANNEL_DISHWASHER_REMAINING_PROGRAM_TIME_STATE)) {
                if (program.getOptions().stream()
                        .noneMatch(option -> OPTION_REMAINING_PROGRAM_TIME.equals(option.getKey()))) {
                    return UnDefType.NULL;
                } else {
                    Option option = program.getOptions().stream()
                            .filter(o -> OPTION_REMAINING_PROGRAM_TIME.equals(o.getKey())).findFirst().get();
                    return createState(channelUID, option.getValue());
                }
            } else if (channelUID.getId().equals(CHANNEL_DISHWASHER_PROGRAM_PROGRESS_STATE)) {
                if (program.getOptions().stream()
                        .noneMatch(option -> OPTION_PROGRAM_PROGRESS.equals(option.getKey()))) {
                    return UnDefType.NULL;
                } else {
                    Option option = program.getOptions().stream()
                            .filter(o -> OPTION_PROGRAM_PROGRESS.equals(o.getKey())).findFirst().get();
                    return createState(channelUID, option.getValue());
                }
            }
        }
        return UnDefType.NULL;
    }

    /**
     * Create channel specific {@link State} based on model value.
     *
     * @param channelUID
     * @param value      model value
     * @return {@link UnDefType} NULL or channel specific state
     */
    private State createState(ChannelUID channelUID, @Nullable String value) {
        if (value != null) {
            if (channelUID.getId().equals(CHANNEL_DISHWASHER_OPERATION_STATE)) {
                return new StringType(mapStringType(value));
            } else if (channelUID.getId().equals(CHANNEL_DISHWASHER_DOOR_STATE)) {
                return STATE_DOOR_OPEN.equals(value) ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
            } else if (channelUID.getId().equals(CHANNEL_DISHWASHER_POWER_STATE)) {
                return STATE_POWER_ON.equals(value) ? OnOffType.ON : OnOffType.OFF;
            } else if (channelUID.getId().equals(CHANNEL_DISHWASHER_REMOTE_START_ALLOWANCE_STATE)
                    || channelUID.getId().equals(CHANNEL_DISHWASHER_REMOTE_CONTROL_ACTIVE_STATE)) {
                return "true".equals(value) ? OnOffType.ON : OnOffType.OFF;
            } else if (channelUID.getId().equals(CHANNEL_DISHWASHER_ACTIVE_PROGRAM_STATE)) {
                return new StringType(mapStringType(value));
            } else if (channelUID.getId().equals(CHANNEL_DISHWASHER_REMAINING_PROGRAM_TIME_STATE)
                    || channelUID.getId().equals(CHANNEL_DISHWASHER_PROGRAM_PROGRESS_STATE)) {
                return new DecimalType(value);
            }
        }

        return UnDefType.NULL;
    }

    /**
     * Map Home Connect key and value names to label.
     * e.g. Dishcare.Dishwasher.Program.Eco50 --> Eco50 or BSH.Common.EnumType.OperationState.DelayedStart --> Delayed
     * Start
     *
     * @param type
     * @return
     */
    private String mapStringType(String type) {
        int index = type.lastIndexOf(".");
        if (index > 0 && type.length() > index) {
            String sub = type.substring(index + 1);
            StringBuilder sb = new StringBuilder();
            for (String word : sub.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
                sb.append(" ");
                sb.append(word);
            }
            return sb.toString().trim();
        }
        return type;
    }

    /**
     * Check bridge status and refresh connection status of thing accordingly.
     */
    private void refreshConnectionStatus() {
        if (client != null) {
            try {
                HomeAppliance homeAppliance = client.getHomeAppliance(getThingHaId());
                if (homeAppliance == null || !homeAppliance.isConnected()) {
                    updateStatus(ThingStatus.OFFLINE);
                } else {
                    updateStatus(ThingStatus.ONLINE);
                }
            } catch (ConfigurationException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            } catch (CommunicationException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
        }
    }

    /**
     * Get home appliance id of Thing.
     *
     * @return home appliance id
     */
    private String getThingHaId() {
        return getThing().getConfiguration().get("haId").toString();
    }

}
