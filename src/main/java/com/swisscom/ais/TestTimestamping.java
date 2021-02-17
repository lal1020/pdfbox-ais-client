package com.swisscom.ais;

import com.swisscom.ais.client.impl.AisClientImpl;
import com.swisscom.ais.client.model.PdfHandle;
import com.swisscom.ais.client.model.SignatureResult;
import com.swisscom.ais.client.model.UserData;
import com.swisscom.ais.client.rest.RestClientConfiguration;
import com.swisscom.ais.client.rest.RestClientImpl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

public class TestTimestamping {

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        properties.load(TestTimestamping.class.getResourceAsStream("/local-config.properties"));

        RestClientConfiguration config = new RestClientConfiguration();
        config.setFromProperties(properties);

        RestClientImpl restClient = new RestClientImpl();
        restClient.setConfiguration(config);

        try (AisClientImpl aisClient = new AisClientImpl(restClient)) {
            UserData userData = new UserData();
            userData.setFromProperties(properties);

            PdfHandle document1 = new PdfHandle();
            document1.setInputFromFile(properties.getProperty("local.test.inputFile"));
            document1.setOutputToFile(properties.getProperty("local.test.outputFilePrefix") + System.currentTimeMillis() + ".pdf");

            SignatureResult result = aisClient.timestamp(Collections.singletonList(document1), userData);
            System.out.println("Final result: " + result);
        }
    }

}
