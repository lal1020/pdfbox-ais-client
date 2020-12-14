package com.swisscom.ais;

import com.swisscom.ais.pdfbox.CreateSignedTimeStamp;

import java.io.File;
import java.util.Properties;

public class TestPdfBoxDirectly {

    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        config.load(TestPdfBoxDirectly.class.getResourceAsStream("/local-config.properties"));

        testTimestampingWithPublicTsa(config);
    }

    // ----------------------------------------------------------------------------------------------------

    public static void testTimestampingWithPublicTsa(Properties config) throws Exception {
        File inFile = new File(config.getProperty("local.directTest.inputFile"));
        File outFile = new File(config.getProperty("local.directTest.outputFilePrefix") + System.currentTimeMillis() + ".pdf");
        if (outFile.exists()) {
            if (!outFile.delete()) {
                System.out.println("Cannot delete the output file " + outFile.getAbsolutePath());
            }
        }

        CreateSignedTimeStamp signing = new CreateSignedTimeStamp(config.getProperty("local.directTest.tsaUrl"));
        signing.signDetached(inFile, outFile);
    }

}
