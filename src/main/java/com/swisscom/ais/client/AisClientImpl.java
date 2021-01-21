package com.swisscom.ais.client;

import com.swisscom.ais.client.model.PdfHandle;
import com.swisscom.ais.client.model.SignatureMode;
import com.swisscom.ais.client.model.UserData;
import com.swisscom.ais.client.rest.RestClient;
import com.swisscom.ais.client.rest.model.AdditionalProfile;
import com.swisscom.ais.client.rest.model.DigestAlgorithm;
import com.swisscom.ais.client.rest.model.ModelHelper;
import com.swisscom.ais.client.rest.model.PdfDocument;
import com.swisscom.ais.client.rest.model.ResponseHelper;
import com.swisscom.ais.client.rest.model.SignatureType;
import com.swisscom.ais.client.rest.model.pendingreq.AISPendingRequest;
import com.swisscom.ais.client.rest.model.signreq.AISSignRequest;
import com.swisscom.ais.client.rest.model.signresp.AISSignResponse;
import com.swisscom.ais.client.rest.model.signresp.ScExtendedSignatureObject;
import com.swisscom.ais.client.utils.Loggers;
import com.swisscom.ais.client.utils.Trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AisClientImpl implements AisClient {

    private static final Logger logClient = LoggerFactory.getLogger(Loggers.CLIENT);
    private static final Logger logProtocol = LoggerFactory.getLogger(Loggers.CLIENT_PROTOCOL);

    private RestClient restClient;
    private AisClientConfiguration configuration = new AisClientConfiguration();

    // ----------------------------------------------------------------------------------------------------

    public AisClientImpl() {
        // no code here
    }

    public AisClientImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    // ----------------------------------------------------------------------------------------------------

    @Override
    public void signWithStaticCertificate(List<PdfHandle> documentHandles, UserData userData) {
        userData.validateYourself();
        Trace trace = new Trace(userData.getTransactionId());
        documentHandles.forEach(handle -> handle.validateYourself(trace));
        // prepare documents
        List<PdfDocument> documentsToSign = prepareMultipleDocumentsForSigning(documentHandles,
                                                                               SignatureMode.STATIC,
                                                                               SignatureType.CMS,
                                                                               trace);
        // start the signature
        AISSignResponse signResponse;
        try {
            List<AdditionalProfile> additionalProfiles = prepareAdditionalProfiles(documentsToSign);
            AISSignRequest signRequest = ModelHelper.buildAisSignRequest(documentsToSign, SignatureType.CMS,
                                                                         userData, additionalProfiles,
                                                                         false, false);
            signResponse = restClient.requestSignature(signRequest, trace);
        } catch (Exception e) {
            throw new AisClientException("Failed to communicate with the AIS service and obtain the signature(s) - " + trace.getId(), e);
        }
        checkThatResponseIsSuccessful(signResponse, trace);
        // finish the signing
        finishDocumentsSigning(documentsToSign, signResponse, SignatureMode.STATIC, trace);
    }

    @Override
    public void signWithOnDemandCertificate(List<PdfHandle> documentHandles, UserData userData) {
        userData.validateYourself();
        Trace trace = new Trace(userData.getTransactionId());
        documentHandles.forEach(handle -> handle.validateYourself(trace));
        // prepare documents
        List<PdfDocument> documentsToSign = prepareMultipleDocumentsForSigning(documentHandles,
                                                                               SignatureMode.ON_DEMAND,
                                                                               SignatureType.CMS,
                                                                               trace);
        // start the signature
        AISSignResponse signResponse;
        try {
            List<AdditionalProfile> additionalProfiles = prepareAdditionalProfiles(documentsToSign, AdditionalProfile.ON_DEMAND_CERTIFICATE);
            AISSignRequest signRequest = ModelHelper.buildAisSignRequest(documentsToSign, SignatureType.CMS,
                                                                         userData, additionalProfiles,
                                                                         false, true);
            signResponse = restClient.requestSignature(signRequest, trace);
        } catch (Exception e) {
            throw new AisClientException("Failed to communicate with the AIS service and obtain the signature(s) - " + trace.getId(), e);
        }
        checkThatResponseIsSuccessful(signResponse, trace);
        // finish the signing
        finishDocumentsSigning(documentsToSign, signResponse, SignatureMode.ON_DEMAND, trace);
    }

    @Override
    public void signWithOnDemandCertificateAndStepUp(List<PdfHandle> documentHandles, UserData userData) {
        userData.validateYourself();
        Trace trace = new Trace(userData.getTransactionId());
        documentHandles.forEach(handle -> handle.validateYourself(trace));
        // prepare documents
        List<PdfDocument> documentsToSign = prepareMultipleDocumentsForSigning(documentHandles,
                                                                               SignatureMode.ON_DEMAND,
                                                                               SignatureType.CMS,
                                                                               trace);
        // start the signature
        AISSignResponse signResponse;
        try {
            List<AdditionalProfile> additionalProfiles = prepareAdditionalProfiles(documentsToSign,
                                                                                   AdditionalProfile.ON_DEMAND_CERTIFICATE,
                                                                                   AdditionalProfile.REDIRECT,
                                                                                   AdditionalProfile.ASYNC);
            AISSignRequest signRequest = ModelHelper.buildAisSignRequest(documentsToSign, SignatureType.CMS,
                                                                         userData, additionalProfiles, true, true);
            signResponse = restClient.requestSignature(signRequest, trace);
        } catch (Exception e) {
            throw new AisClientException("Failed to communicate with the AIS service and obtain the signature(s) - " + trace.getId(), e);
        }
        checkThatResponseIsPending(signResponse, trace);
        // poll for signature status
        signResponse = pollUntilSignatureIsComplete(signResponse, userData, trace);
        checkThatResponseIsSuccessful(signResponse, trace);
        // finish the signing
        finishDocumentsSigning(documentsToSign, signResponse, SignatureMode.ON_DEMAND, trace);
    }

    @Override
    public void timestamp(List<PdfHandle> documentHandles, UserData userData) {
        userData.validateYourself();
        Trace trace = new Trace(userData.getTransactionId());
        documentHandles.forEach(handle -> handle.validateYourself(trace));
        // prepare documents
        List<PdfDocument> documentsToSign = prepareMultipleDocumentsForSigning(documentHandles,
                                                                               SignatureMode.TIMESTAMP,
                                                                               SignatureType.TIMESTAMP,
                                                                               trace);
        AISSignResponse signResponse;
        try {
            List<AdditionalProfile> additionalProfiles = prepareAdditionalProfiles(documentsToSign, AdditionalProfile.TIMESTAMP);
            AISSignRequest signRequest = ModelHelper.buildAisSignRequest(documentsToSign, SignatureType.TIMESTAMP,
                                                                         userData, additionalProfiles,
                                                                         false, false);
            signResponse = restClient.requestSignature(signRequest, trace);
        } catch (Exception e) {
            throw new AisClientException("Failed to communicate with the AIS service and obtain the signature(s) - " + trace.getId(), e);
        }
        checkThatResponseIsSuccessful(signResponse, trace);
        // finish the signing
        finishDocumentsSigning(documentsToSign, signResponse, SignatureMode.TIMESTAMP, trace);
    }

    @Override
    public void close() throws IOException {
        if (restClient != null) {
            restClient.close();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    @SuppressWarnings("unused")
    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @SuppressWarnings("unused")
    public AisClientConfiguration getConfiguration() {
        return configuration;
    }

    @SuppressWarnings("unused")
    public void setConfiguration(AisClientConfiguration configuration) {
        this.configuration = configuration;
    }

    // ----------------------------------------------------------------------------------------------------

    @SuppressWarnings("SameParameterValue")
    private List<PdfDocument> prepareMultipleDocumentsForSigning(List<PdfHandle> documentHandles,
                                                                 SignatureMode signatureMode,
                                                                 SignatureType signatureType,
                                                                 Trace trace) {
        return documentHandles
            .stream()
            .map(handle -> prepareOneDocumentForSigning(handle, signatureMode, signatureType, trace))
            .collect(Collectors.toList());
    }

    private PdfDocument prepareOneDocumentForSigning(PdfHandle documentHandle, SignatureMode signatureMode,
                                                     SignatureType signatureType, Trace trace) {
        try {
            if (logClient.isDebugEnabled()) {
                logClient.debug("Preparing {} signing for document: {} - {}",
                                signatureMode.getFriendlyName(),
                                documentHandle.getInputFromFile(),
                                trace);
            }
            FileInputStream fileIn = new FileInputStream(documentHandle.getInputFromFile());
            FileOutputStream fileOut = new FileOutputStream(documentHandle.getOutputToFile());

            PdfDocument newDocument = new PdfDocument(documentHandle.getOutputToFile(), fileIn, fileOut);
            newDocument.prepareForSigning(DigestAlgorithm.SHA512, signatureType);
            return newDocument;
        } catch (Exception e) {
            throw new AisClientException("Failed to prepare the document [" +
                                         documentHandle.getInputFromFile() + "] for " +
                                         signatureMode.getFriendlyName() + " signing", e);
        }
    }

    private List<AdditionalProfile> prepareAdditionalProfiles(List<PdfDocument> documentsToSign, AdditionalProfile... extraProfiles) {
        List<AdditionalProfile> additionalProfiles = new ArrayList<>();
        if (documentsToSign.size() > 1) {
            additionalProfiles.add(AdditionalProfile.BATCH);
        }
        additionalProfiles.addAll(Arrays.asList(extraProfiles));
        return additionalProfiles;
    }

    private void checkThatResponseIsSuccessful(AISSignResponse signResponse, Trace trace) {
        if (!ResponseHelper.responseIsMajorSuccess(signResponse)) {
            throw new AisClientException("Failure response received from AIS service: " +
                                         ResponseHelper.getResponseResultSummary(signResponse) + " - " + trace.getId());
        }
    }

    private void checkThatResponseIsPending(AISSignResponse signResponse, Trace trace) {
        if (!ResponseHelper.responseIsAsyncPending(signResponse)) {
            throw new AisClientException("Failure response received from AIS service: " +
                                         ResponseHelper.getResponseResultSummary(signResponse) + " - " + trace.getId());
        }
    }

    private boolean checkForConsentUrlInTheResponse(AISSignResponse response, UserData userData, Trace trace) {
        if (ResponseHelper.responseHasStepUpConsentUrl(response)) {
            if (userData.getConsentUrlCallback() != null) {
                userData.getConsentUrlCallback().onConsentUrlReceived(ResponseHelper.getStepUpConsentUrl(response), userData);
            } else {
                logClient.warn("Consent URL was received from AIS, but no consent URL callback was configured " +
                               "(in UserData). This transaction will probably fail - {}", trace.getId());
            }
            return true;
        }
        return false;
    }

    private AISSignResponse pollUntilSignatureIsComplete(AISSignResponse signResponse, UserData userData, Trace trace) {
        AISSignResponse localResponse = signResponse;
        try {
            if (checkForConsentUrlInTheResponse(localResponse, userData, trace)) {
                TimeUnit.SECONDS.sleep(configuration.getSignaturePollingIntervalInSeconds());
            }
            for (int round = 0; round < configuration.getSignaturePollingRounds(); round++) {
                logProtocol.debug("Polling for signature status, round {}/{} - {}",
                                  round + 1, configuration.getSignaturePollingRounds(), trace.getId());
                AISPendingRequest pendingRequest = ModelHelper.buildAisPendingRequest(ResponseHelper.getResponseId(localResponse), userData);
                localResponse = restClient.pollForSignatureStatus(pendingRequest, trace);
                checkForConsentUrlInTheResponse(localResponse, userData, trace);
                if (ResponseHelper.responseIsAsyncPending(localResponse)) {
                    TimeUnit.SECONDS.sleep(configuration.getSignaturePollingIntervalInSeconds());
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            throw new AisClientException("Failed to poll AIS for the status of the signature(s) - " + trace.getId(), e);
        }
        return localResponse;
    }

    private void finishDocumentsSigning(List<PdfDocument> documentsToSign, AISSignResponse signResponse,
                                        SignatureMode signatureMode, Trace trace) {
        try {
            if (signatureMode == SignatureMode.TIMESTAMP) {
                if (documentsToSign.size() == 1) {
                    PdfDocument document = documentsToSign.get(0);
                    logClient.info("Finalizing the timestamping for document: {} - {}", document.getName(), trace.getId());
                    String base64TimestampToken = signResponse.getSignResponse().getSignatureObject().getTimestamp().getRFC3161TimeStampToken();
                    document.getPbSigningSupport().setSignature(Base64.getDecoder().decode(base64TimestampToken));
                    document.close();
                } else {
                    for (PdfDocument document : documentsToSign) {
                        logClient.info("Finalizing the timestamping for document: {} - {}", document.getName(), trace.getId());
                        ScExtendedSignatureObject signatureObject = ResponseHelper.getSignatureObjectByDocumentId(document.getId(), signResponse);
                        document.getPbSigningSupport().setSignature(
                            Base64.getDecoder().decode(signatureObject.getTimestamp().getRFC3161TimeStampToken()));
                        document.close();
                    }
                }
            } else {
                if (documentsToSign.size() == 1) {
                    PdfDocument document = documentsToSign.get(0);
                    logClient.info("Finalizing the signature for document: {} - {}", document.getName(), trace.getId());
                    document.getPbSigningSupport().setSignature(
                        Base64.getDecoder().decode(signResponse.getSignResponse().getSignatureObject().getBase64Signature().get$()));
                    document.close();
                } else {
                    for (PdfDocument document : documentsToSign) {
                        logClient.info("Finalizing the signature for document: {} - {}", document.getName(), trace.getId());
                        ScExtendedSignatureObject signatureObject = ResponseHelper.getSignatureObjectByDocumentId(document.getId(), signResponse);
                        document.getPbSigningSupport().setSignature(Base64.getDecoder().decode(signatureObject.getBase64Signature().get$()));
                        document.close();
                    }
                }
            }
        } catch (Exception e) {
            throw new AisClientException("Failed to embed the signature(s) in the document(s) and close the streams - " + trace.getId(), e);
        }
    }

}
