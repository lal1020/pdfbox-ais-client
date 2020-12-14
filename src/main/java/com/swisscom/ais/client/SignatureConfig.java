package com.swisscom.ais.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;

public class SignatureConfig {

    private String clientKeyFile;
    private String clientKeyPassword;

    private String clientCertificateFile;
    private String serverCertificateFile;

    private String claimedIdentityName;
    private String distinguishedName;
    private String documentDigest;
    private String documentDigestAlgorithm;

    private String promptLanguage;
    private String promptMsisdn;
    private String promptMessage;

    private String[] additionalProfiles;

    // ----------------------------------------------------------------------------------------------------

    public String getClientKeyFile() {
        return clientKeyFile;
    }

    public void setClientKeyFile(String clientKeyFile) {
        this.clientKeyFile = clientKeyFile;
    }

    public String getClientKeyPassword() {
        return clientKeyPassword;
    }

    public void setClientKeyPassword(String clientKeyPassword) {
        this.clientKeyPassword = clientKeyPassword;
    }

    public String getClientCertificateFile() {
        return clientCertificateFile;
    }

    public void setClientCertificateFile(String clientCertificateFile) {
        this.clientCertificateFile = clientCertificateFile;
    }

    public String getServerCertificateFile() {
        return serverCertificateFile;
    }

    public void setServerCertificateFile(String serverCertificateFile) {
        this.serverCertificateFile = serverCertificateFile;
    }

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
        clientKeyFile = getNotNull(properties, "client.auth.keyFile");
        clientKeyPassword = getNotNull(properties, "client.auth.keyPassword");
        clientCertificateFile = getNotNull(properties, "client.cert.file");
        serverCertificateFile = getNotNull(properties, "server.cert.file");
        claimedIdentityName = getNotNull(properties, "signature.claimedIdentityName");
        documentDigest = getNotNull(properties, "document.digest");
        documentDigestAlgorithm = getNotNull(properties, "document.digestAlgorithm");
        promptLanguage = getNotNull(properties, "signature.prompt.language");
        promptMsisdn = getNotNull(properties, "signature.prompt.msisdn");
        promptMessage = getNotNull(properties, "signature.prompt.message");
        distinguishedName = getNotNull(properties, "signature.distinguishedName");
        additionalProfiles = getStringArray(getNotNull(properties, "signature.additionalProfilesCsv"));
    }

    private String getNotNull(Properties properties, String propertyName) {
        String value = properties.getProperty(propertyName);
        if (value == null) {
            throw new IllegalStateException("Invalid configuration. The [" + propertyName + "] is missing or is empty");
        }
        return value;
    }

    private String[] getStringArray(String csv) {
        StringTokenizer tokenizer = new StringTokenizer(csv, ",");
        String[] result = new String[tokenizer.countTokens()];
        for (int index = 0; index < result.length; index++) {
            result[index] = tokenizer.nextToken().trim();
        }
        return result;
    }

}
