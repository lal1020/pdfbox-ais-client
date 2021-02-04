package com.swisscom.ais.client.model;

public enum SignatureMode {

    TIMESTAMP("Timestamp"),
    STATIC("Static"),
    ON_DEMAND("On Demand"),
    ON_DEMAND_STEP_UP("On Demand with Step Up");

    private final String friendlyName;

    SignatureMode(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

}
