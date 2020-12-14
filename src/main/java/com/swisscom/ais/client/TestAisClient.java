package com.swisscom.ais.client;

import com.swisscom.ais.client.rest.model.signresp.AISSignResponse;

import java.util.concurrent.TimeUnit;

public class TestAisClient {

    public static void main(String[] args) throws Exception {
        SignatureConfig config = new SignatureConfig();
        config.loadFromPropertiesClasspathFile("/local-config.properties");

        try (AisClient client = new AisClient()) {
            client.initialize(config);
            AISSignResponse response = client.requestSignature(config);
            while (CoreValues.responseIsAsyncPending(response)) {
                if (CoreValues.responseHasStepUpConsentUrl(response)) {
                    System.out.println("Consent URL: " + CoreValues.getStepUpConsentUrl(response));
                }
                Thread.sleep(TimeUnit.SECONDS.toMillis(3));
                String responseId = response.getSignResponse().getOptionalOutputs().getAsyncResponseID();
                response = client.pollForSignatureStatus(config, responseId);
            }
            System.out.println(response);
        }
    }

}
