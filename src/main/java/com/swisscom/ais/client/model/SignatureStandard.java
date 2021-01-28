package com.swisscom.ais.client.model;

public enum SignatureStandard {

    /**
     * The default signature standard that is used. This is normally CAdES but the actual decision is left to the AIS service.
     */
    DEFAULT(""),

    /**
     * CAdES compliant signature.
     */
    CADES("CAdES"),

    /**
     * Formerly named PAdES: Adds to the CMS a revocation info archival attribute as described in the PDF reference.
     */
    PDF("PDF"),

    /**
     * Alias for PDF for backward compatibility.
     *
     * @deprecated Please use the {@link #PDF} element.
     */
    @Deprecated
    PADES("PAdES"),

    /**
     * PAdES compliant signature, which returns the revocation information as optional output.
     * In order to get an LTV-enabled PDF signature, the client must process the optional output and fill the PDF's DSS.
     */
    PADES_BASELINE("PAdES-baseline"),

    /**
     * Plain signature, which returns revocation information as optional output.
     */
    PLAIN("PLAIN");

    private final String value;

    SignatureStandard(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
