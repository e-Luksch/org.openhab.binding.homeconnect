/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeconnect.internal.client.model;

/**
 * Event model
 *
 * @author Jonas Brüstel - Initial contribution
 *
 */
public class Event {

    private String key;
    private String value;
    private String unit;

    public Event(String key, String value, String unit) {
        this.key = key;
        this.value = value;
        this.unit = unit;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public boolean getValueAsBoolean() {
        return value != null ? Boolean.valueOf(getValue()).booleanValue() : false;
    }

    public int getValueAsInt() {
        return value != null ? Integer.valueOf(getValue()).intValue() : 0;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return "Event [key=" + key + ", value=" + value + ", unit=" + unit + "]";
    }

}
