/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeconnect.internal.handler;

import static org.eclipse.smarthome.core.library.unit.SmartHomeUnits.PERCENT;
import static org.openhab.binding.homeconnect.internal.HomeConnectBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.homeconnect.internal.client.exception.CommunicationException;
import org.openhab.binding.homeconnect.internal.client.exception.ConfigurationException;
import org.openhab.binding.homeconnect.internal.client.model.Program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomeConnectCoffeeMakerHandler} is responsible for handling commands, which are
 * sent to one of the channels of a coffee machine.
 *
 * @author Jonas Brüstel - Initial contribution
 * @author Lukas Werner
 */
@NonNullByDefault
public class HomeConnectCoffeeMakerHandler extends AbstractHomeConnectThingHandler {

    private final Logger logger = LoggerFactory.getLogger(HomeConnectCoffeeMakerHandler.class);

    public HomeConnectCoffeeMakerHandler(Thing thing) {
        super(thing);
        // event handler
        registerEventHandler(EVENT_REMOTE_CONTROL_START_ALLOWED,
                defaultBooleanEventHandler(CHANNEL_REMOTE_START_ALLOWANCE_STATE));
        registerEventHandler(EVENT_DOOR_STATE, defaultDoorStateEventHandler());
        registerEventHandler(EVENT_OPERATION_STATE, defaultOperationStateEventHandler());
        registerEventHandler(EVENT_REMOTE_CONTROL_ACTIVE,
                defaultBooleanEventHandler(CHANNEL_REMOTE_CONTROL_ACTIVE_STATE));
        registerEventHandler(EVENT_ACTIVE_PROGRAM, event -> {
            getThingChannel(CHANNEL_ACTIVE_PROGRAM_STATE).ifPresent(channel -> {
                updateState(channel.getUID(),
                        event.getValue() == null ? UnDefType.NULL : new StringType(mapStringType(event.getValue())));

                // revert other channels
                if (event.getValue() == null) {
                    resetProgramStateChannels();
                } else {
                    // get progress etc. from API
                    updateChannel(channel.getUID());
                }
            });
        });
        registerEventHandler(EVENT_POWER_STATE, defaultPowerStateEventHandler());
        registerEventHandler(EVENT_PROGRAM_PROGRESS, defaultProgramProgressEventHandler());
        registerEventHandler(EVENT_SELECTED_PROGRAM, defaultSelectedProgramStateEventHandler());

        registerEventHandler(EVENT_DISCONNECTED, event -> {
            resetAllChannels();
        });
        registerEventHandler(EVENT_CONNECTED, event -> {
            // revert active program states
            resetProgramStateChannels();

            // refresh all channels
            updateChannels();
        });

        registerEventHandler(EVENT_POWER_STATE, event -> {
            getThingChannel(CHANNEL_POWER_STATE).ifPresent(channel -> updateState(channel.getUID(),
                    STATE_POWER_ON.equals(event.getValue()) ? OnOffType.ON : OnOffType.OFF));

            if (!STATE_POWER_ON.equals(event.getValue())) {
                resetProgramStateChannels();
                getThingChannel(CHANNEL_SELECTED_PROGRAM_STATE).ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
                getThingChannel(CHANNEL_ACTIVE_PROGRAM_STATE).ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
            }

        });

        // register update handlers
        registerChannelUpdateHandler(CHANNEL_POWER_STATE, defaultPowerStateChannelUpdateHandler());
        registerChannelUpdateHandler(CHANNEL_REMOTE_START_ALLOWANCE_STATE,
                defaultRemoteStartAllowanceChannelUpdateHandler());
        registerChannelUpdateHandler(CHANNEL_DOOR_STATE, defaultDoorStateChannelUpdateHandler());
        registerChannelUpdateHandler(CHANNEL_OPERATION_STATE, defaultOperationStateChannelUpdateHandler());
        registerChannelUpdateHandler(CHANNEL_ACTIVE_PROGRAM_STATE, (channelUID, client) -> {
            Program program = client.getActiveProgram(getThingHaId());
            if (program != null && program.getKey() != null) {
                updateState(channelUID, new StringType(mapStringType(program.getKey())));
                program.getOptions().forEach(option -> {
                    switch (option.getKey()) {
                        case OPTION_PROGRAM_PROGRESS:
                            getThingChannel(CHANNEL_PROGRAM_PROGRESS_STATE)
                                    .ifPresent(channel -> updateState(channel.getUID(),
                                            option.getValueAsInt() == 100 ? UnDefType.NULL
                                                    : new QuantityType<>(option.getValueAsInt(), PERCENT)));
                            break;
                    }
                });
            } else {
                updateState(channelUID, UnDefType.NULL);
                resetProgramStateChannels();
            }
        });
        registerChannelUpdateHandler(CHANNEL_SELECTED_PROGRAM_STATE, (channelUID, client) -> {
            Program program = client.getSelectedProgram(getThingHaId());
            if (program != null && program.getKey() != null) {
                updateState(channelUID, new StringType(mapStringType(program.getKey())));
            } else {
                updateState(channelUID, UnDefType.NULL);
                resetProgramStateChannels();
            }
        });
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        super.handleCommand(channelUID, command);

        if (command instanceof OnOffType && CHANNEL_POWER_STATE.equals(channelUID.getId())) {
            try {
                // turn coffeemaker on and off
                getClient().setPowerState(getThingHaId(),
                        OnOffType.ON.equals(command) ? STATE_POWER_ON : STATE_POWER_STANDBY);
            } catch (ConfigurationException | CommunicationException e) {
                logger.error("API communication problem!", e);
            }
        }
    }

    @Override
    public String toString() {
        return "HomeConnectCoffeeMakerHandler [haId: " + getThingHaId() + "]";
    }

    @Override
    protected void powerStateOffCleanup() {
        logger.debug("ProgramStates will be resettet, during power-off");
        resetProgramStateChannels();
    }

    private void resetProgramStateChannels() {
        logger.debug("Resetting active program channel states");
        getThingChannel(CHANNEL_PROGRAM_PROGRESS_STATE).ifPresent(c -> updateState(c.getUID(), UnDefType.NULL));
    }

    private void resetAllChannels() {
        logger.debug("Resetting all channels");
        getThing().getChannels().forEach(channel -> {
            if (!CHANNEL_POWER_STATE.equals(channel.getUID().getId())) {
                updateState(channel.getUID(), UnDefType.NULL);
            }
        });
    }
}
