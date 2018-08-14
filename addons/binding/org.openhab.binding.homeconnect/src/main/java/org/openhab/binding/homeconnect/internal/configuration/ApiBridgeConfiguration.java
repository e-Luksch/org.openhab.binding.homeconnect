/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeconnect.internal.configuration;

/**
 * The {@link ApiBridgeConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Jonas Brüstel - Initial contribution
 */
public class ApiBridgeConfiguration {

    private String clientId;
    private String clientSecret;
    private String token;
    private String refreshToken;
    private boolean simulator;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public boolean isSimulator() {
        return simulator;
    }

    public void setSimulator(boolean simulator) {
        this.simulator = simulator;
    }

    @Override
    public String toString() {
        return "ApiBridgeConfiguration [clientId=" + clientId + ", clientSecret=" + clientSecret + ", token=" + token
                + ", refreshToken=" + refreshToken + ", simulator=" + simulator + "]";
    }

}
