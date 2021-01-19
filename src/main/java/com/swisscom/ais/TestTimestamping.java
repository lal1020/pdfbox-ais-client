package com.swisscom.ais;

import com.swisscom.ais.client.AisClientImpl;
import com.swisscom.ais.client.model.PdfHandle;
import com.swisscom.ais.client.model.UserData;
import com.swisscom.ais.client.rest.RestClientConfiguration;
import com.swisscom.ais.client.rest.RestClientImpl;

import java.util.Properties;

public class TestTimestamping {

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        properties.load(TestPdfBoxDirectly.class.getResourceAsStream("/local-config.properties"));

        RestClientConfiguration config = new RestClientConfiguration();
        config.setFromProperties(properties);

        RestClientImpl restClient = new RestClientImpl();
        restClient.setConfiguration(config);

        try (AisClientImpl aisClient = new AisClientImpl()) {
            UserData userData = new UserData();
            userData.setFromProperties(properties);

            PdfHandle document = new PdfHandle();
            document.setInputFromFile(properties.getProperty("local.test.inputFile"));
            document.setOutputToFile(properties.getProperty("local.test.outputFilePrefix") + System.currentTimeMillis() + ".pdf");
            aisClient.timestampOneSingleDocument(document, userData);
        }
    }

}
