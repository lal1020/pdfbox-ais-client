package com.swisscom.ais.client.rest.model;

import com.swisscom.ais.client.model.UserData;
import com.swisscom.ais.client.rest.model.pendingreq.AISPendingRequest;
import com.swisscom.ais.client.rest.model.pendingreq.AsyncPendingRequest;
import com.swisscom.ais.client.rest.model.signreq.*;
import com.swisscom.ais.client.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModelHelper {

    private static final String SWISSCOM_BASIC_PROFILE = "http://ais.swisscom.ch/1.1";

    public static AISSignRequest buildAisSignRequest(List<PdfDocument> documents,
                                                     SignatureType signatureType,
                                                     UserData userData,
                                                     List<AdditionalProfile> additionalProfiles,
                                                     boolean withStepUp) {
        // Input documents --------------------------------------------------------------------------------
        List<DocumentHash> documentHashes = new ArrayList<>();
        for (PdfDocument document : documents) {
            DocumentHash newDocumentHash = new DocumentHash();
            newDocumentHash.setId(document.getId());
            newDocumentHash.setDsigDigestMethod(new DsigDigestMethod().withAlgorithm(document.getDigestAlgorithm().getDigestUri()));
            newDocumentHash.setDsigDigestValue(document.getBase64HashToSign());
            documentHashes.add(newDocumentHash);
        }
        InputDocuments inputDocuments = new InputDocuments();
        inputDocuments.setDocumentHash(documentHashes);

        // Optional inputs --------------------------------------------------------------------------------
//        AddTimestamp addTimestamp = new AddTimestamp();
//        addTimestamp.setType(CoreValues.TIMESTAMP_TYPE_RFC_3161); // TODO

        ClaimedIdentity claimedIdentity = new ClaimedIdentity();
        claimedIdentity.setName(userData.getClaimedIdentityName());

        ScCertificateRequest certificateRequest = null;
        if (withStepUp) {
            ScPhone phone = new ScPhone();
            phone.setScLanguage(userData.getPromptLanguage());
            phone.setScMSISDN(userData.getPromptMsisdn());
            phone.setScMessage(userData.getPromptMessage());

            ScStepUpAuthorisation stepUpAuthorisation = new ScStepUpAuthorisation();
            stepUpAuthorisation.setScPhone(phone);

            certificateRequest = new ScCertificateRequest();
            certificateRequest.setScDistinguishedName(userData.getDistinguishedName());
            certificateRequest.setScStepUpAuthorisation(stepUpAuthorisation);
        }

        OptionalInputs optionalInputs = new OptionalInputs();
//        optionalInputs.setAddTimestamp(addTimestamp);
        optionalInputs.setAdditionalProfile(additionalProfiles.stream().map(AdditionalProfile::getUri).collect(Collectors.toList()));
        optionalInputs.setClaimedIdentity(claimedIdentity);
        optionalInputs.setSignatureType(signatureType.getUri());
//        optionalInputs.setScAddRevocationInformation("");
        optionalInputs.setScCertificateRequest(certificateRequest);

        // Sign request --------------------------------------------------------------------------------
        SignRequest request = new SignRequest();
        request.setRequestID(Utils.generateRequestId());
        request.setProfile(SWISSCOM_BASIC_PROFILE);
        request.setInputDocuments(inputDocuments);
        request.setOptionalInputs(optionalInputs);

        AISSignRequest requestWrapper = new AISSignRequest();
        requestWrapper.setSignRequest(request);
        return requestWrapper;
    }

    public static AISPendingRequest buildAisPendingRequest(String responseId, UserData userData) {
        com.swisscom.ais.client.rest.model.pendingreq.ClaimedIdentity claimedIdentity =
            new com.swisscom.ais.client.rest.model.pendingreq.ClaimedIdentity();
        claimedIdentity.setName(userData.getClaimedIdentityName());

        com.swisscom.ais.client.rest.model.pendingreq.OptionalInputs optionalInputs =
            new com.swisscom.ais.client.rest.model.pendingreq.OptionalInputs();
        optionalInputs.setAsyncResponseID(responseId);
        optionalInputs.setClaimedIdentity(claimedIdentity);

        AsyncPendingRequest request = new AsyncPendingRequest();
        request.setProfile(SWISSCOM_BASIC_PROFILE);
        request.setOptionalInputs(optionalInputs);

        AISPendingRequest requestWrapper = new AISPendingRequest();
        requestWrapper.setAsyncPendingRequest(request);
        return requestWrapper;
    }

}
