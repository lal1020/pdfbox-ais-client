package com.swisscom.ais.client.rest.model;

import com.swisscom.ais.client.AisClientException;
import com.swisscom.ais.client.rest.model.signresp.AISSignResponse;
import com.swisscom.ais.client.rest.model.signresp.Result;
import com.swisscom.ais.client.rest.model.signresp.ScExtendedSignatureObject;
import com.swisscom.ais.client.rest.model.signresp.ScSignatureObjects;

public class ResponseHelper {

    public static boolean responseIsAsyncPending(AISSignResponse response) {
        if (response != null && response.getSignResponse() != null && response.getSignResponse().getResult() != null) {
            return ResultMajorCode.PENDING.getUri().equals(response.getSignResponse().getResult().getResultMajor());
        }
        return false;
    }

    public static boolean responseIsMajorSuccess(AISSignResponse response) {
        return response != null &&
               response.getSignResponse() != null &&
               response.getSignResponse().getResult() != null &&
               ResultMajorCode.SUCCESS.getUri().equals(response.getSignResponse().getResult().getResultMajor());
    }

    public static String getResponseResultSummary(AISSignResponse response) {
        Result result = response.getSignResponse().getResult();
        return "Major=[" + result.getResultMajor() + "], "
               + "Minor=[" + result.getResultMinor() + "], "
               + "Message=[" + result.getResultMessage() + ']';
    }

    public static boolean responseHasStepUpConsentUrl(AISSignResponse response) {
        return response != null &&
               response.getSignResponse() != null &&
               response.getSignResponse().getOptionalOutputs() != null &&
               response.getSignResponse().getOptionalOutputs().getScStepUpAuthorisationInfo() != null &&
               response.getSignResponse().getOptionalOutputs().getScStepUpAuthorisationInfo().getScResult() != null &&
               response.getSignResponse().getOptionalOutputs().getScStepUpAuthorisationInfo().getScResult().getScConsentURL() != null;
    }

    public static String getStepUpConsentUrl(AISSignResponse response) {
        return response.getSignResponse().getOptionalOutputs().getScStepUpAuthorisationInfo().getScResult().getScConsentURL();
    }

    public static String getResponseId(AISSignResponse response) {
        return response.getSignResponse().getOptionalOutputs().getAsyncResponseID();
    }

    public static ScExtendedSignatureObject getSignatureObjectByDocumentId(String documentId, AISSignResponse signResponse) {
        ScSignatureObjects signatureObjects = signResponse.getSignResponse().getSignatureObject().getOther().getScSignatureObjects();
        for (ScExtendedSignatureObject seSignatureObject : signatureObjects.getScExtendedSignatureObject()) {
            if (documentId.equals(seSignatureObject.getWhichDocument())) {
                return seSignatureObject;
            }
        }
        throw new AisClientException("Invalid AIS response. Cannot find the extended signature object for document with ID=[" + documentId + "]");
    }

}
