package com.swisscom.ais.client.model;

public enum RevocationInformation {

    /**
     * For CMS signatures, if the attribute for revocation information is not provided (set as default),
     * the revocation information will match the defined SignatureStandard (CAdES or PAdES).
     * (In case the SignatureStandard is not set, the default is CAdES).
     *
     * For timestamps, the signature standard does not apply so the revocation information type must be
     * set explicitly.
     */
    DEFAULT(""),

    /**
     * RI will be embedded as an unsigned attribute with OID 1.2.840.113549.1.9.16.2.24.
     */
    CADES("CAdES"),

    /**
     * CMS Signatures: RI will be embedded in the signature as a signed attribute with OID 1.2.840.113583.1.1.8.
     * Trusted Timestamps: RI will be provided in the response as Base64 encoded OCSP responses or CRLs
     * within the <OptionalOutputs>-Element.
     */
    PADES("PAdES"),

    /**
     * Both RI types (CAdES,PAdES) will be provided.
     */
    BOTH("BOTH");

    private final String value;

    RevocationInformation(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
