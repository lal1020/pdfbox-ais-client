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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AisClientImpl implements AisClient {

    private static final Logger logClient = LoggerFactory.getLogger(Loggers.CLIENT);
    private static final Logger logProtocol = LoggerFactory.getLogger(Loggers.CLIENT_PROTOCOL);
    private static final Logger logReqResp = LoggerFactory.getLogger(Loggers.REQUEST_RESPONSE);
    private static final Logger logFullReqResp = LoggerFactory.getLogger(Loggers.FULL_REQUEST_RESPONSE);

    private RestClient restClient;

    // ----------------------------------------------------------------------------------------------------

    public AisClientImpl() {
        // no code here
    }

    public AisClientImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    // ----------------------------------------------------------------------------------------------------

    @Override
    public void signDocumentsWithStaticCertificate(List<PdfHandle> documentHandles, UserData userData) {
        userData.validateYourself();
        Trace trace = new Trace(userData.getTransactionId());
        throw new UnsupportedOperationException("This operation is not implemented yet"); // TODO
    }

    @Override
    public void signDocumentsWithOnDemandCertificate(List<PdfHandle> documentHandles, UserData userData) {
        userData.validateYourself();
        Trace trace = new Trace(userData.getTransactionId());
        // prepare documents
        List<PdfDocument> documentsToSign = prepareMultipleDocumentsForSigning(documentHandles,
                                                                               SignatureMode.ON_DEMAND,
                                                                               SignatureType.CMS,
                                                                               trace);
        // start the signature
        AISSignResponse signResponse;
        try {
            List<AdditionalProfile> additionalProfiles = Arrays.asList(AdditionalProfile.ON_DEMAND_CERTIFICATE,
                                                                       AdditionalProfile.REDIRECT,
                                                                       AdditionalProfile.ASYNC);
            if (documentsToSign.size() > 1) {
                additionalProfiles.add(AdditionalProfile.BATCH);
            }
            AISSignRequest signRequest = ModelHelper.buildAisSignRequest(documentsToSign, SignatureType.CMS,
                                                                         userData, additionalProfiles, true);
            signResponse = restClient.requestSignature(signRequest, trace);
        } catch (Exception e) {
            throw new AisClientException("Failed to communicate with the AIS service and obtain the signature(s) - " + trace.getId(), e);
        }
        checkThatResponseIsPending(signResponse, trace);

        // poll for signature status
        signResponse = pollUntilSignatureIsComplete(signResponse, userData, trace);
        checkThatResponseIsSuccessful(signResponse, trace);

        // finalize document(s) signing
        try {
            if (documentsToSign.size() == 1) {
                PdfDocument document = documentsToSign.get(0);
                document.getPbSigningSupport().setSignature(Base64.getDecoder().decode(
                    signResponse.getSignResponse().getSignatureObject().getBase64Signature().get$()));
                document.close();
            } else {
                for (PdfDocument document : documentsToSign) {
                    ScExtendedSignatureObject signatureObject = ResponseHelper.getSignatureObjectByDocumentId(document.getId(), signResponse);
                    // TODO signatureObject here also contains the type of the signature, which should be considered as well
                    document.getPbSigningSupport().setSignature(Base64.getDecoder().decode(signatureObject.getBase64Signature().get$()));
                    document.close();
                }
            }
        } catch (Exception e) {
            throw new AisClientException("Failed to embed the signature(s) in the document(s) and close the streams - " + trace.getId(), e);
        }
    }

    @Override
    public void timestampOneSingleDocument(PdfHandle documentHandle, UserData userData) {
        userData.validateYourself();
        Trace trace = new Trace(userData.getTransactionId());
        PdfDocument documentToSign = prepareOneDocumentForSigning(documentHandle,
                                                                  SignatureMode.TIMESTAMP,
                                                                  SignatureType.TIMESTAMP,
                                                                  trace);
        AISSignResponse signResponse;
        try {
            AISSignRequest signRequest = ModelHelper.buildAisSignRequest(Collections.singletonList(documentToSign),
                                                                         SignatureType.TIMESTAMP,
                                                                         userData,
                                                                         Collections.singletonList(AdditionalProfile.TIMESTAMP),
                                                                         false);
            signResponse = restClient.requestSignature(signRequest, trace);
        } catch (Exception e) {
            throw new AisClientException("Failed to communicate with the AIS service and obtain the signature(s) - " + trace.getId(), e);
        }

        checkThatResponseIsSuccessful(signResponse, trace);

        try {
            String base64TimestampToken = signResponse.getSignResponse().getSignatureObject().getTimestamp().getRFC3161TimeStampToken();
            documentToSign.getPbSigningSupport().setSignature(Base64.getDecoder().decode(base64TimestampToken));
            documentToSign.close();
        } catch (Exception e) {
            throw new AisClientException("Failed to embed the signature(s) in the document(s) and close the streams", e);
        }
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

    // ----------------------------------------------------------------------------------------------------

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

            PdfDocument newDocument = new PdfDocument(fileIn, fileOut);
            newDocument.prepareForSigning(DigestAlgorithm.SHA512, signatureType);
            return newDocument;
        } catch (Exception e) {
            throw new AisClientException("Failed to prepare the document [" +
                                         documentHandle.getInputFromFile() + "] for " +
                                         signatureMode.getFriendlyName() + " signing", e);
        }
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

    private AISSignResponse pollUntilSignatureIsComplete(AISSignResponse signResponse, UserData userData, Trace trace) {
        AISSignResponse localResponse = signResponse;
        try {
            if (ResponseHelper.responseIsAsyncPending(localResponse)) {
                do {
                    if (ResponseHelper.responseHasStepUpConsentUrl(localResponse)) {
                        if (userData.getConsentUrlCallback() != null) {
                            userData.getConsentUrlCallback().onConsentUrlReceived(ResponseHelper.getStepUpConsentUrl(localResponse), userData);
                        } else {
                            logClient.warn("Consent URL was received from AIS, but no consent URL callback was configured " +
                                           "(in UserData). This transaction will probably fail - {}", trace.getId());
                        }
                    }
                    TimeUnit.SECONDS.sleep(5);
                    AISPendingRequest pendingRequest = ModelHelper.buildAisPendingRequest(ResponseHelper.getResponseId(localResponse), userData);
                    localResponse = restClient.pollForSignatureStatus(pendingRequest, trace);
                } while (ResponseHelper.responseIsAsyncPending(localResponse));
            }
        } catch (Exception e) {
            throw new AisClientException("Failed to poll AIS for the status of the signature(s)", e);
        }
        return localResponse;
    }

}
