package com.swisscom.ais;

import com.swisscom.ais.client.AisClientImpl;
import com.swisscom.ais.client.model.PdfHandle;
import com.swisscom.ais.client.model.UserData;
import com.swisscom.ais.client.rest.RestClientConfiguration;
import com.swisscom.ais.client.rest.RestClientImpl;

import java.util.Arrays;
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
            userData.setTransactionIdToRandomUuid();

            PdfHandle document1 = new PdfHandle();
            document1.setInputFromFile(properties.getProperty("local.test.inputFile1"));
            document1.setOutputToFile(properties.getProperty("local.test.outputFilePrefix1") + System.currentTimeMillis() + ".pdf");

            PdfHandle document2 = new PdfHandle();
            document2.setInputFromFile(properties.getProperty("local.test.inputFile2"));
            document2.setOutputToFile(properties.getProperty("local.test.outputFilePrefix2") + System.currentTimeMillis() + ".pdf");

            aisClient.timestamp(Arrays.asList(document1, document2), userData);
        }
    }

}
