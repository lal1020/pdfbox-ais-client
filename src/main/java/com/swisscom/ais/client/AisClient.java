package com.swisscom.ais.client;

import com.swisscom.ais.client.model.PdfHandle;
import com.swisscom.ais.client.model.UserData;

import java.io.Closeable;
import java.util.List;

// TODO handle the normal failure outcomes (user cancel, user timeout, etc)
public interface AisClient extends Closeable {

    void signWithStaticCertificate(List<PdfHandle> documentHandles, UserData userData) throws AisClientException;

    void signWithOnDemandCertificate(List<PdfHandle> documentHandles, UserData userData) throws AisClientException;

    void signWithOnDemandCertificateAndStepUp(List<PdfHandle> documentHandles, UserData userData) throws AisClientException;

    void timestamp(List<PdfHandle> documentHandles, UserData userData) throws AisClientException;

}
