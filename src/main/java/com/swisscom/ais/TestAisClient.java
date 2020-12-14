package com.swisscom.ais;

import com.swisscom.ais.client.AisClientImpl;
import com.swisscom.ais.client.model.PdfDocument;
import com.swisscom.ais.client.rest.RestClientConfiguration;
import com.swisscom.ais.client.rest.RestClientImpl;

import java.util.Collections;

public class TestAisClient {

    public static void main(String[] args) throws Exception {
        RestClientConfiguration config = new RestClientConfiguration();
        config.setFromPropertiesClasspathFile("/local-config.properties");

        RestClientImpl restClient = new RestClientImpl();
        restClient.setConfiguration(config);

        AisClientImpl aisClient = new AisClientImpl();
        aisClient.setRestClient(restClient);

        try {
            PdfDocument document = new PdfDocument();
            document.setInputFromFile("D:/Projects/Swisscom/AIS/Tests/local-sample-doc.pdf");
            document.setOutputToFile("D:/Projects/Swisscom/AIS/Tests/test-timestamp" + System.currentTimeMillis() + ".pdf");
            aisClient.timestampDocuments(Collections.singletonList(document));
        } finally {
            aisClient.close();
        }
    }

}
