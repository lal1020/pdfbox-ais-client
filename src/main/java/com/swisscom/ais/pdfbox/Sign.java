package com.swisscom.ais.pdfbox;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

public class Sign {

    public static boolean externalSigning = false;

    public static void main(String[] args) throws Exception {
        File inFile = new File("D:/Projects/Swisscom/AIS/test-runs/local-sample-doc.pdf");
        File outFile = new File("D:/Projects/Swisscom/AIS/test-runs/local-out.pdf");
        if (outFile.exists()) {
            if (!outFile.delete()) {
                System.out.println("Cannot delete the output file " + outFile.getAbsolutePath());
            }
        }

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        char[] password = "secret".toCharArray();
        keystore.load(new FileInputStream("D:/Projects/Swisscom/AIS/Documents/test-runs/bogdan-mocanu.jks"), password);

        // sign PDF
//        CreateSignature signing = new CreateSignature(keystore, password);
//        signing.setExternalSigning(true);
//        signing.signDetached(inFile, outFile, null);
        CreateSignedTimeStamp signing = new CreateSignedTimeStamp("https://freetsa.org/tsr");
        signing.signDetached(inFile, outFile);
    }
}
