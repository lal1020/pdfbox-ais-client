package com.swisscom.ais.client.model;

public enum SignatureStandard {

    /**
     * The default signature standard is used. This is normally CAdES but the actual decision is left to the AIS service.
     */
    DEFAULT(""),

    CADES("CAdES"),

    PADES("PAdES");

    private final String value;

    SignatureStandard(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
