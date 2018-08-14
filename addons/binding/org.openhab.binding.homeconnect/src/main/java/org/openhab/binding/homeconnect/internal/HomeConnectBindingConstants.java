/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeconnect.internal;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link HomeConnectBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Jonas Brüstel - Initial contribution
 */
@NonNullByDefault
public class HomeConnectBindingConstants {

    private static final String BINDING_ID = "homeconnect";

    public static final String HA_ID = "haId";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_API_BRIDGE = new ThingTypeUID(BINDING_ID, "api_bridge");
    public static final ThingTypeUID THING_TYPE_DISHWASHER = new ThingTypeUID(BINDING_ID, "dishwasher");

    // List of all Channel ids
    public static final String CHANNEL_DISHWASHER_POWER_STATE = "dishwasher_power_state";
    public static final String CHANNEL_DISHWASHER_DOOR_STATE = "dishwasher_door_state";
    public static final String CHANNEL_DISHWASHER_OPERATION_STATE = "dishwasher_operation_state";
    public static final String CHANNEL_DISHWASHER_REMOTE_START_ALLOWANCE_STATE = "dishwasher_remote_start_allowance_state";
    public static final String CHANNEL_DISHWASHER_REMOTE_CONTROL_ACTIVE_STATE = "dishwasher_remote_control_active_state";
    public static final String CHANNEL_DISHWASHER_ACTIVE_PROGRAM_STATE = "dishwasher_active_program_state";
    public static final String CHANNEL_DISHWASHER_REMAINING_PROGRAM_TIME_STATE = "dishwasher_remaining_program_time_state";
    public static final String CHANNEL_DISHWASHER_PROGRAM_PROGRESS_STATE = "dishwasher_program_progress_state";

    // List of all supported devices
    public static final Set<ThingTypeUID> SUPPORTED_DEVICE_THING_TYPES_UIDS = Stream
            .of(THING_TYPE_API_BRIDGE, THING_TYPE_DISHWASHER).collect(Collectors.toSet());

    // List of state values
    public static final String STATE_POWER_OFF = "BSH.Common.EnumType.PowerState.Off";
    public static final String STATE_POWER_ON = "BSH.Common.EnumType.PowerState.On";
    public static final String STATE_POWER_STANDBY = "BSH.Common.EnumType.PowerState.Standby";
    public static final String STATE_DOOR_OPEN = "BSH.Common.EnumType.DoorState.Open";
    public static final String STATE_DOOR_LOCKED = "BSH.Common.EnumType.DoorState.Locked";
    public static final String STATE_DOOR_CLOSED = "BSH.Common.EnumType.DoorState.Closed";

    // List of state keys
    public static final String STATE_POWER = "BSH.Common.Setting.PowerState";
    public static final String STATE_DOOR = "BSH.Common.Status.DoorState";
    public static final String STATE_OPERATION = "BSH.Common.Status.OperationState";
    public static final String STATE_REMOTE_START = "BSH.Common.Status.RemoteControlStartAllowed";
    public static final String STATE_REMOTE_CONTROL = "BSH.Common.Status.RemoteControlActive";
    public static final String STATE_ACTIVE_PROGRAM = "BSH.Common.Root.ActiveProgram";

    // List of options
    public static final String OPTION_REMAINING_PROGRAM_TIME = "BSH.Common.Option.RemainingProgramTime";
    public static final String OPTION_PROGRAM_PROGRESS = "BSH.Common.Option.ProgramProgress";

}
