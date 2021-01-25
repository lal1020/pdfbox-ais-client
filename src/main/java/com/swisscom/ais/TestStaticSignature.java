package com.swisscom.ais;

import com.swisscom.ais.client.AisClientImpl;
import com.swisscom.ais.client.model.PdfHandle;
import com.swisscom.ais.client.model.RevocationInformation;
import com.swisscom.ais.client.model.SignatureResult;
import com.swisscom.ais.client.model.UserData;
import com.swisscom.ais.client.rest.RestClientConfiguration;
import com.swisscom.ais.client.rest.RestClientImpl;

import java.util.Collections;
import java.util.Properties;

public class TestStaticSignature {

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        properties.load(TestStaticSignature.class.getResourceAsStream("/local-config.properties"));

        RestClientConfiguration config = new RestClientConfiguration();
        config.setFromProperties(properties);

        RestClientImpl restClient = new RestClientImpl();
        restClient.setConfiguration(config);

        try (AisClientImpl aisClient = new AisClientImpl(restClient)) {
            UserData userData = new UserData();
            userData.setFromProperties(properties);
            userData.setConsentUrlCallback((consentUrl, userData1) -> System.out.println("Consent URL: " + consentUrl));
            userData.setAddTimestamp(true);
            userData.setAddRevocationInformation(RevocationInformation.PADES);

            PdfHandle document = new PdfHandle();
            document.setInputFromFile(properties.getProperty("local.test.inputFile"));
            document.setOutputToFile(properties.getProperty("local.test.outputFilePrefix") + System.currentTimeMillis() + ".pdf");

            SignatureResult result = aisClient.signWithStaticCertificate(Collections.singletonList(document), userData);
            System.out.println("Final result: " + result);
        }
    }

}
