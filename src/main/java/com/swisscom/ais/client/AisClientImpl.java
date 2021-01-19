package com.swisscom.ais.client;

import com.swisscom.ais.client.model.PdfHandle;
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AisClientImpl implements AisClient {

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
        throw new UnsupportedOperationException("This operation is not implemented yet"); // TODO
    }

    @Override
    public void signDocumentsWithOnDemandCertificate(List<PdfHandle> documentHandles, UserData userData) {
        // prepare documents
        List<PdfDocument> documentsToSign = prepareDocumentsForSigning(documentHandles, SignatureType.CMS);

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
            signResponse = restClient.requestSignature(signRequest);
        } catch (Exception e) {
            throw new AisClientException("Failed to communicate with the AIS service and obtain the signature(s)", e);
        }

        checkThatResponseIsPending(signResponse);

        signResponse = pollUntilSignatureIsComplete(signResponse, userData);
        checkThatResponseIsSuccessful(signResponse);

        try {
            for (PdfDocument document : documentsToSign) {
                ScExtendedSignatureObject signatureObject = ResponseHelper.getSignatureObjectByDocumentId(document.getId(), signResponse);
                // TODO signatureObject here also contains the type of the signature, which should be considered as well
                document.getPbSigningSupport().setSignature(Base64.getDecoder().decode(signatureObject.getBase64Signature().get$()));
                document.close();
            }
        } catch (Exception e) {
            throw new AisClientException("Failed to embed the signature(s) in the document(s) and close the streams", e);
        }
    }

    @Override
    public void timestampOneSingleDocument(PdfHandle documentHandle, UserData userData) {
        List<PdfDocument> documentsToSign = prepareDocumentsForSigning(Collections.singletonList(documentHandle), SignatureType.TIMESTAMP);

        AISSignResponse signResponse;
        try {
            List<AdditionalProfile> additionalProfiles = Collections.singletonList(AdditionalProfile.TIMESTAMP);
            AISSignRequest signRequest = ModelHelper.buildAisSignRequest(documentsToSign, SignatureType.TIMESTAMP,
                                                                         userData, additionalProfiles, false);
            signResponse = restClient.requestSignature(signRequest);
        } catch (Exception e) {
            throw new AisClientException("Failed to communicate with the AIS service and obtain the signature(s)", e);
        }

        checkThatResponseIsSuccessful(signResponse);

        try {
            String base64TimestampToken = signResponse.getSignResponse().getSignatureObject().getTimestamp().getRFC3161TimeStampToken();
            PdfDocument document = documentsToSign.get(0);
            document.getPbSigningSupport().setSignature(Base64.getDecoder().decode(base64TimestampToken));
            document.close();
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

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    // ----------------------------------------------------------------------------------------------------

    private List<PdfDocument> prepareDocumentsForSigning(List<PdfHandle> documentHandles, SignatureType signatureType) {
        List<PdfDocument> documentsToSign = new ArrayList<>(documentHandles.size());
        try {
            for (PdfHandle handle : documentHandles) {
                FileInputStream fileIn = new FileInputStream(handle.getInputFromFile());
                FileOutputStream fileOut = new FileOutputStream(handle.getOutputToFile());

                PdfDocument newDocument = new PdfDocument(fileIn, fileOut);
                newDocument.prepareForSigning(DigestAlgorithm.SHA512, signatureType);

                documentsToSign.add(newDocument);
            }
        } catch (Exception e) {
            throw new AisClientException("Failed to prepare the documents for signing", e);
        }
        return documentsToSign;
    }

    private void checkThatResponseIsSuccessful(AISSignResponse signResponse) {
        if (!ResponseHelper.responseIsMajorSuccess(signResponse)) {
            throw new AisClientException("Failure response received from AIS service: " + ResponseHelper.getResponseResultSummary(signResponse));
        }
    }

    private void checkThatResponseIsPending(AISSignResponse signResponse) {
        if (!ResponseHelper.responseIsAsyncPending(signResponse)) {
            throw new AisClientException("Failure response received from AIS service: " + ResponseHelper.getResponseResultSummary(signResponse));
        }
    }

    private AISSignResponse pollUntilSignatureIsComplete(AISSignResponse signResponse, UserData userData) {
        AISSignResponse localResponse = signResponse;
        try {
            if (ResponseHelper.responseIsAsyncPending(localResponse)) {
                do {
                    if (ResponseHelper.responseHasStepUpConsentUrl(localResponse)) {
                        // TODO
                        System.out.println("Consent URL: [" + ResponseHelper.getStepUpConsentUrl(localResponse) + "]");
                    }
                    TimeUnit.SECONDS.sleep(5);
                    AISPendingRequest pendingRequest = ModelHelper.buildAisPendingRequest(ResponseHelper.getResponseId(localResponse), userData);
                    localResponse = restClient.pollForSignatureStatus(pendingRequest);
                } while (ResponseHelper.responseIsAsyncPending(localResponse));
            }
        } catch (Exception e) {
            throw new AisClientException("Failed to poll AIS for the status of the signature(s)", e);
        }
        return localResponse;
    }

}
