package com.swisscom.ais.client.model;

import com.swisscom.ais.client.AisClientException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import static com.swisscom.ais.client.utils.Utils.getStringNotNull;

public class UserData {

    private String transactionId;

    private String claimedIdentityName;
    private String distinguishedName;

    private String promptLanguage;
    private String promptMsisdn;
    private String promptMessage;

    private ConsentUrlCallback consentUrlCallback;

    // ----------------------------------------------------------------------------------------------------

    public UserData() {
    }

    public UserData(String transactionId) {
        this.transactionId = transactionId;
    }

    // ----------------------------------------------------------------------------------------------------

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void setTransactionIdToRandomUuid() {
        this.transactionId = UUID.randomUUID().toString();
    }

    public String getClaimedIdentityName() {
        return claimedIdentityName;
    }

    public void setClaimedIdentityName(String claimedIdentityName) {
        this.claimedIdentityName = claimedIdentityName;
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

    public ConsentUrlCallback getConsentUrlCallback() {
        return consentUrlCallback;
    }

    public void setConsentUrlCallback(ConsentUrlCallback consentUrlCallback) {
        this.consentUrlCallback = consentUrlCallback;
    }

    // ----------------------------------------------------------------------------------------------------

    public void setFromPropertiesClasspathFile(String fileName) {
        Properties properties;
        try {
            properties = new Properties();
            properties.load(this.getClass().getResourceAsStream(fileName));
        } catch (IOException exception) {
            throw new AisClientException("Failed to load user data properties from classpath file: [" + fileName + "]", exception);
        }
        setFromProperties(properties);
    }

    public void setFromPropertiesFile(String fileName) {
        Properties properties;
        try {
            properties = new Properties();
            properties.load(new FileInputStream(fileName));
        } catch (IOException exception) {
            throw new AisClientException("Failed to load user data properties from file: [" + fileName + "]", exception);
        }
        setFromProperties(properties);
    }

    public void setFromProperties(Properties properties) {
        claimedIdentityName = getStringNotNull(properties, "signature.claimedIdentityName");
        promptLanguage = getStringNotNull(properties, "signature.prompt.language");
        promptMsisdn = getStringNotNull(properties, "signature.prompt.msisdn");
        promptMessage = getStringNotNull(properties, "signature.prompt.message");
        distinguishedName = getStringNotNull(properties, "signature.distinguishedName");
    }

    public void validateYourself() {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new AisClientException("The user data's transactionId cannot be null or empty. For example, you can set it to a new UUID.");
        }
    }

}
