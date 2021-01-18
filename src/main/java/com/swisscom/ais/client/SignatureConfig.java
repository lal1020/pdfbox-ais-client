package com.swisscom.ais.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static com.swisscom.ais.client.utils.Utils.getNotNull;
import static com.swisscom.ais.client.utils.Utils.getStringArray;

public class SignatureConfig {

    private String claimedIdentityName;
    private String distinguishedName;
    private String documentDigest;
    private String documentDigestAlgorithm;

    private String promptLanguage;
    private String promptMsisdn;
    private String promptMessage;

    private String[] additionalProfiles;

    // ----------------------------------------------------------------------------------------------------

    public String getClaimedIdentityName() {
        return claimedIdentityName;
    }

    public void setClaimedIdentityName(String claimedIdentityName) {
        this.claimedIdentityName = claimedIdentityName;
    }

    public String getDocumentDigest() {
        return documentDigest;
    }

    public void setDocumentDigest(String documentDigest) {
        this.documentDigest = documentDigest;
    }

    public String getDocumentDigestAlgorithm() {
        return documentDigestAlgorithm;
    }

    public void setDocumentDigestAlgorithm(String documentDigestAlgorithm) {
        this.documentDigestAlgorithm = documentDigestAlgorithm;
    }

    public String getPromptLanguage() {
        return promptLanguage;
    }

    public void setPromptLanguage(String promptLanguage) {
        this.promptLanguage = promptLanguage;
    }

    public String getPromptMsisdn() {
        return promptMsisdn;
    }

    public void setPromptMsisdn(String promptMsisdn) {
        this.promptMsisdn = promptMsisdn;
    }

    public String getPromptMessage() {
        return promptMessage;
    }

    public void setPromptMessage(String promptMessage) {
        this.promptMessage = promptMessage;
    }

    public String getDistinguishedName() {
        return distinguishedName;
    }

    public void setDistinguishedName(String distinguishedName) {
        this.distinguishedName = distinguishedName;
    }

    public String[] getAdditionalProfiles() {
        return additionalProfiles;
    }

    public void setAdditionalProfiles(String[] additionalProfiles) {
        this.additionalProfiles = additionalProfiles;
    }

    // ----------------------------------------------------------------------------------------------------

    public void loadFromPropertiesClasspathFile(String fileName) {
        try {
            loadFromPropertiesInputStream(this.getClass().getResourceAsStream(fileName));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void loadFromPropertiesFile(String fileName) {
        try {
            loadFromPropertiesInputStream(new FileInputStream(fileName));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    // ----------------------------------------------------------------------------------------------------

    private void loadFromPropertiesInputStream(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        claimedIdentityName = getNotNull(properties, "signature.claimedIdentityName");
        documentDigest = getNotNull(properties, "document.digest");
        documentDigestAlgorithm = getNotNull(properties, "document.digestAlgorithm");
        promptLanguage = getNotNull(properties, "signature.prompt.language");
        promptMsisdn = getNotNull(properties, "signature.prompt.msisdn");
        promptMessage = getNotNull(properties, "signature.prompt.message");
        distinguishedName = getNotNull(properties, "signature.distinguishedName");
        additionalProfiles = getStringArray(getNotNull(properties, "signature.additionalProfilesCsv"));
    }

}
