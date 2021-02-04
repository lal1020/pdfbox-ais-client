package com.swisscom.ais;

import com.swisscom.ais.client.AisClientConfiguration;
import com.swisscom.ais.client.impl.AisClientImpl;
import com.swisscom.ais.client.model.PdfHandle;
import com.swisscom.ais.client.model.RevocationInformation;
import com.swisscom.ais.client.model.SignatureResult;
import com.swisscom.ais.client.model.SignatureStandard;
import com.swisscom.ais.client.model.UserData;
import com.swisscom.ais.client.rest.RestClientConfiguration;
import com.swisscom.ais.client.rest.RestClientImpl;
import com.swisscom.ais.client.rest.model.DigestAlgorithm;

import java.io.IOException;
import java.util.Collections;

/**
 * Test that shows how to configure the REST and AIS clients from the code. This can also be switched to configuration via the Spring framework or
 * other similar DI frameworks.
 */
public class TestFullyProgrammaticConfiguration {

    public static void main(String[] args) throws IOException {
        // configuration for the REST client; this is done once per application lifetime
        RestClientConfiguration restConfig = new RestClientConfiguration();
        restConfig.setRestServiceSignUrl("https://ais.swisscom.com/AIS-Server/rs/v1.0/sign");
        restConfig.setRestServicePendingUrl("https://ais.swisscom.com/AIS-Server/rs/v1.0/pending");
        restConfig.setServerCertificateFile("/home/user/ais-server.crt");
        restConfig.setClientKeyFile("/home/user/ais-client.key");
        restConfig.setClientKeyPassword("secret");
        restConfig.setClientCertificateFile("/home/user/ais-client.crt");

        RestClientImpl restClient = new RestClientImpl();
        restClient.setConfiguration(restConfig);

        // then configure the AIS client; this is done once per application lifetime
        AisClientConfiguration aisConfig = new AisClientConfiguration();
        aisConfig.setSignaturePollingIntervalInSeconds(10);
        aisConfig.setSignaturePollingRounds(10);

        try (AisClientImpl aisClient = new AisClientImpl(aisConfig, restClient)) {
            // third, configure a UserData instance with details about this signature
            // this is done for each signature (can also be created once and cached on a per-user basis)
            UserData userData = new UserData();
            userData.setClaimedIdentityName("ais-90days-trial");
            userData.setClaimedIdentityKey("keyEntity");
            userData.setDistinguishedName("cn=TEST User, givenname=Max, surname=Maximus, c=US, serialnumber=abcdefabcdefabcdefabcdefabcdef");

            userData.setStepUpLanguage("en");
            userData.setStepUpMessage("Please confirm the signing of the document");
            userData.setStepUpMsisdn("0040799999999");

            userData.setSignatureReason("For testing purposes");
            userData.setSignatureLocation("Topeka, Kansas");
            userData.setSignatureContactInfo("test@test.com");

            userData.setSignatureStandard(SignatureStandard.PADES);

            userData.setConsentUrlCallback((consentUrl, userData1) -> System.out.println("Consent URL: " + consentUrl));

            // fourth, populate a PdfHandle with details about the document to be signed. More than one PdfHandle can be given
            PdfHandle document = new PdfHandle();
            document.setInputFromFile("/home/user/input.pdf");
            document.setOutputToFile("/home/user/signed-output.pdf");
            document.setDigestAlgorithm(DigestAlgorithm.SHA256);

            // finally, do the signature
            SignatureResult result = aisClient.signWithOnDemandCertificateAndStepUp(Collections.singletonList(document), userData);
            if (result == SignatureResult.SUCCESS) {
                // yay!
            }
        }
    }

}
