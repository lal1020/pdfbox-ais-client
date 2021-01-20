package com.swisscom.ais.client.model;

import com.swisscom.ais.client.AisClientException;
import com.swisscom.ais.client.rest.model.RevocationInformation;
import com.swisscom.ais.client.rest.model.SignatureStandard;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import static com.swisscom.ais.client.utils.Utils.getStringNotNull;

@SuppressWarnings("unused")
public class UserData {

    private String transactionId;

    private String claimedIdentityName;
    private String claimedIdentityKey;
    private String distinguishedName;

    private String stepUpLanguage;
    private String stepUpMsisdn;
    private String stepUpMessage;
    private String stepUpSerialNumber;

    private ConsentUrlCallback consentUrlCallback;

    private boolean addTimestamp = true;
    private RevocationInformation addRevocationInformation = RevocationInformation.DEFAULT;
    private SignatureStandard signatureStandard = SignatureStandard.DEFAULT;

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

    public String getStepUpLanguage() {
        return stepUpLanguage;
    }

    public void setStepUpLanguage(String stepUpLanguage) {
        this.stepUpLanguage = stepUpLanguage;
    }

    public String getStepUpMsisdn() {
        return stepUpMsisdn;
    }

    public void setStepUpMsisdn(String stepUpMsisdn) {
        this.stepUpMsisdn = stepUpMsisdn;
    }

    public String getStepUpMessage() {
        return stepUpMessage;
    }

    public void setStepUpMessage(String stepUpMessage) {
        this.stepUpMessage = stepUpMessage;
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

    public boolean isAddTimestamp() {
        return addTimestamp;
    }

    public void setAddTimestamp(boolean addTimestamp) {
        this.addTimestamp = addTimestamp;
    }

    public RevocationInformation getAddRevocationInformation() {
        return addRevocationInformation;
    }

    public void setAddRevocationInformation(RevocationInformation addRevocationInformation) {
        this.addRevocationInformation = addRevocationInformation;
    }

    public SignatureStandard getSignatureStandard() {
        return signatureStandard;
    }

    public void setSignatureStandard(SignatureStandard signatureStandard) {
        this.signatureStandard = signatureStandard;
    }

    public String getClaimedIdentityKey() {
        return claimedIdentityKey;
    }

    public void setClaimedIdentityKey(String claimedIdentityKey) {
        this.claimedIdentityKey = claimedIdentityKey;
    }

    public String getStepUpSerialNumber() {
        return stepUpSerialNumber;
    }

    public void setStepUpSerialNumber(String stepUpSerialNumber) {
        this.stepUpSerialNumber = stepUpSerialNumber;
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
        claimedIdentityKey = properties.getProperty("signature.claimedIdentityKey");
        stepUpLanguage = getStringNotNull(properties, "signature.stepUp.language");
        stepUpMsisdn = getStringNotNull(properties, "signature.stepUp.msisdn");
        stepUpMessage = getStringNotNull(properties, "signature.stepUp.message");
        stepUpSerialNumber = getStringNotNull(properties, "signature.stepUp.serialNumber");
        distinguishedName = getStringNotNull(properties, "signature.distinguishedName");
    }

    public void validateYourself() {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new AisClientException("The user data's transactionId cannot be null or empty. For example, you can set it to a new UUID.");
        }
    }

}
