package com.swisscom.ais.client;

import com.swisscom.ais.client.model.PdfHandle;
import com.swisscom.ais.client.model.UserData;

import java.io.Closeable;
import java.util.List;

// TODO handle the normal failure outcomes (user cancel, user timeout, etc)
public interface AisClient extends Closeable {

    void signDocumentsWithStaticCertificate(List<PdfHandle> documentHandles, UserData userData) throws AisClientException;

    void signDocumentsWithOnDemandCertificate(List<PdfHandle> documentHandles, UserData userData) throws AisClientException;

    void timestampOneSingleDocument(PdfHandle documentHandle, UserData userData) throws AisClientException;

}
