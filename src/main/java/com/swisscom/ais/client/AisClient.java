package com.swisscom.ais.client;

import com.swisscom.ais.client.model.PdfHandle;

import java.io.Closeable;
import java.util.List;

public interface AisClient extends Closeable {

    void signDocumentsWithStaticCertificate(List<PdfHandle> documents);

    void signDocumentsWithOnDemandCertificate(List<PdfHandle> documents);

    void timestampDocuments(List<PdfHandle> documents);

}
