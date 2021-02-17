package com.swisscom.ais.client.rest;

import com.swisscom.ais.client.rest.model.pendingreq.AISPendingRequest;
import com.swisscom.ais.client.rest.model.signreq.AISSignRequest;
import com.swisscom.ais.client.rest.model.signresp.AISSignResponse;
import com.swisscom.ais.client.utils.Trace;

import java.io.Closeable;

public interface RestClient extends Closeable {

    AISSignResponse requestSignature(AISSignRequest request, Trace trace);

    AISSignResponse pollForSignatureStatus(AISPendingRequest request, Trace trace);

}
