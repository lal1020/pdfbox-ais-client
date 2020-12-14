package com.swisscom.ais.client;

import com.swisscom.ais.client.rest.model.signresp.AISSignResponse;

public class CoreValues {

    public static final String SWISSCOM_BASIC_PROFILE = "http://ais.swisscom.ch/1.1";
    public static final String AIS_REST_SIGN_URL = "https://ais.swisscom.com/AIS-Server/rs/v1.0/sign";
    public static final String AIS_REST_PENDING_URL = "https://ais.swisscom.com/AIS-Server/rs/v1.0/pending";

    public static final String DIGEST_ALGO_SHA512 = "http://www.w3.org/2001/04/xmlenc#sha512";
    public static final String TIMESTAMP_TYPE_RFC_3161 = "urn:ietf:rfc:3161";
    public static final String SIGNATURE_TYPE_RFC_3369 = "urn:ietf:rfc:3369";

    public static final String PROFILE_BATCH_PROCESSING = "http://ais.swisscom.ch/1.0/profiles/batchprocessing";
    public static final String PROFILE_TIMESTAMPING = "urn:oasis:names:tc:dss:1.0:profiles:timestamping";
    public static final String PROFILE_ON_DEMAND_CERTIFICATE = "http://ais.swisscom.ch/1.0/profiles/ondemandcertificate";
    public static final String PROFILE_ASYNC_PROCESSING = "urn:oasis:names:tc:dss:1.0:profiles:asynchronousprocessing";
    public static final String PROFILE_REDIRECT = "http://ais.swisscom.ch/1.1/profiles/redirect";

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
            return CoreValues.RESULT_MAJOR_ASYNC_PENDING.equals(response.getSignResponse().getResult().getResultMajor());
        }
        return false;
    }

    public static boolean responseIsMajorSuccess(AISSignResponse response) {
        return response != null &&
               response.getSignResponse() != null &&
               response.getSignResponse().getResult() != null &&
               RESULT_MAJOR_SUCCESS.equals(response.getSignResponse().getResult().getResultMajor());
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

}
