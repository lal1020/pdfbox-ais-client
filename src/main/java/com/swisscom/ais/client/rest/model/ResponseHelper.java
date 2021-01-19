package com.swisscom.ais.client.rest.model;

import com.swisscom.ais.client.AisClientException;
import com.swisscom.ais.client.rest.model.signresp.AISSignResponse;
import com.swisscom.ais.client.rest.model.signresp.Result;
import com.swisscom.ais.client.rest.model.signresp.ScExtendedSignatureObject;
import com.swisscom.ais.client.rest.model.signresp.ScSignatureObjects;

public class ResponseHelper {

    public static final String RESULT_MAJOR_SUCCESS = "urn:oasis:names:tc:dss:1.0:resultmajor:Success";
    public static final String RESULT_MAJOR_ASYNC_PENDING = "urn:oasis:names:tc:dss:1.0:profiles:asynchronousprocessing:resultmajor:Pending";
    public static final String RESULT_MAJOR_SUBSYSTEM_ERROR = "http://ais.swisscom.ch/1.0/resultmajor/SubsystemError";

    public static final String RESULT_MINOR_REQUEST_UNEXPECTED_DATA = "http://ais.swisscom.ch/1.0/resultminor/UnexpectedData";
    public static final String RESULT_MINOR_STEPUP_SERVICE = "http://ais.swisscom.ch/1.1/resultminor/subsystem/StepUp/service";
    public static final String RESULT_MINOR_STEPUP_CANCEL = "http://ais.swisscom.ch/1.1/resultminor/subsystem/StepUp/cancel";
    public static final String RESULT_MINOR_STEPUP_SN_MISMATCH = "http://ais.swisscom.ch/1.1/resultminor/subsystem/StepUp/SerialNumberMismatch";

    // ----------------------------------------------------------------------------------------------------

    public static boolean responseIsAsyncPending(AISSignResponse response) {
        if (response != null && response.getSignResponse() != null && response.getSignResponse().getResult() != null) {
            return ResponseHelper.RESULT_MAJOR_ASYNC_PENDING.equals(response.getSignResponse().getResult().getResultMajor());
        }
        return false;
    }

    public static boolean responseIsMajorSuccess(AISSignResponse response) {
        return response != null &&
               response.getSignResponse() != null &&
               response.getSignResponse().getResult() != null &&
               RESULT_MAJOR_SUCCESS.equals(response.getSignResponse().getResult().getResultMajor());
    }

    public static String getResponseResultSummary(AISSignResponse response) {
        Result result = response.getSignResponse().getResult();
        StringBuilder builder = new StringBuilder(300);
        builder.append("Major=[").append(result.getResultMajor()).append("], ");
        builder.append("Minor=[").append(result.getResultMinor()).append("], ");
        builder.append("Message=[").append(result.getResultMessage()).append(']');
        return builder.toString();
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
