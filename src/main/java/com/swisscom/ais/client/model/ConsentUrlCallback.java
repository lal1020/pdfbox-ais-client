package com.swisscom.ais.client.model;

public interface ConsentUrlCallback {

    void onConsentUrlReceived(String consentUrl, UserData userData);

}
