package com.swisscom.ais.client.model;

import com.swisscom.ais.client.AisClientException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static com.swisscom.ais.client.utils.Utils.getNotNull;

public class UserData {

    private String claimedIdentityName;
    private String distinguishedName;

    private String promptLanguage;
    private String promptMsisdn;
    private String promptMessage;

    // ----------------------------------------------------------------------------------------------------

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

    // ----------------------------------------------------------------------------------------------------

    public void setFromPropertiesClasspathFile(String fileName) {
        try {
            Properties properties = new Properties();
            properties.load(this.getClass().getResourceAsStream(fileName));
            setFromProperties(properties);
        } catch (IOException exception) {
            throw new AisClientException("Failed to load user data properties from classpath file: [" + fileName + "]", exception);
        }
    }

    public void setFromPropertiesFile(String fileName) {
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(fileName));
            setFromProperties(properties);
        } catch (IOException exception) {
            throw new AisClientException("Failed to load user data properties from file: [" + fileName + "]", exception);
        }
    }

    public void setFromProperties(Properties properties) {
        claimedIdentityName = getNotNull(properties, "signature.claimedIdentityName");
        promptLanguage = getNotNull(properties, "signature.prompt.language");
        promptMsisdn = getNotNull(properties, "signature.prompt.msisdn");
        promptMessage = getNotNull(properties, "signature.prompt.message");
        distinguishedName = getNotNull(properties, "signature.distinguishedName");
    }

}
