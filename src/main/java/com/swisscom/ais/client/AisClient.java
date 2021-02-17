package com.swisscom.ais.client;

import com.swisscom.ais.client.model.PdfHandle;
import com.swisscom.ais.client.model.SignatureResult;
import com.swisscom.ais.client.model.UserData;

import java.io.Closeable;
import java.util.List;

public interface AisClient extends Closeable {

    SignatureResult signWithStaticCertificate(List<PdfHandle> documentHandles, UserData userData) throws AisClientException;

    SignatureResult signWithOnDemandCertificate(List<PdfHandle> documentHandles, UserData userData) throws AisClientException;

    SignatureResult signWithOnDemandCertificateAndStepUp(List<PdfHandle> documentHandles, UserData userData) throws AisClientException;

    SignatureResult timestamp(List<PdfHandle> documentHandles, UserData userData) throws AisClientException;

}
