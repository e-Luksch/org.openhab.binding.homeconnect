/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeconnect.internal.client.exception;

import java.io.IOException;
import java.util.Date;

/**
 * API communication exception
 *
 * @author Jonas Brüstel - Initial contribution
 *
 */
public class CommunicationException extends Exception {

    private static final long serialVersionUID = 1L;

    public CommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommunicationException(String message) {
        super(message);
    }

    public CommunicationException(Throwable cause) {
        super(cause);
    }

    public CommunicationException(int code, String message, String body) throws IOException {
        super(String.format("Communication error! response code: %d, message: %s, body: %s (Tried at %s)", code,
                message, body, new Date()));
    }

}
