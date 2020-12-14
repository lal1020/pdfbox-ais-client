package com.swisscom.ais.client.rest.model;

public class SignatureData {

    private byte[] digestToSign;

    // ----------------------------------------------------------------------------------------------------

    public byte[] getDigestToSign() {
        return digestToSign;
    }

    public void setDigestToSign(byte[] digestToSign) {
        this.digestToSign = digestToSign;
    }
    
}
