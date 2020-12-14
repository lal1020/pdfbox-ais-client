package com.swisscom.ais;

import com.swisscom.ais.client.AisClientImpl;
import com.swisscom.ais.client.model.PdfDocument;
import com.swisscom.ais.client.rest.RestClientConfiguration;
import com.swisscom.ais.client.rest.RestClientImpl;

import java.util.Collections;
import java.util.Properties;

public class TestAisClient {

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        properties.load(TestPdfBoxDirectly.class.getResourceAsStream("/local-config.properties"));

        RestClientConfiguration config = new RestClientConfiguration();
        config.setFromProperties(properties);

        RestClientImpl restClient = new RestClientImpl();
        restClient.setConfiguration(config);

        AisClientImpl aisClient = new AisClientImpl();
        aisClient.setRestClient(restClient);

        try {
            PdfDocument document = new PdfDocument();
            document.setInputFromFile(properties.getProperty("local.test.inputFile"));
            document.setOutputToFile(properties.getProperty("local.test.outputFilePrefix") + System.currentTimeMillis() + ".pdf");
            aisClient.timestampDocuments(Collections.singletonList(document));
        } finally {
            aisClient.close();
        }
    }

}
