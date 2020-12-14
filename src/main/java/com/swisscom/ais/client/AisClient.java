package com.swisscom.ais.client;

import com.swisscom.ais.client.model.PdfDocument;

import java.io.Closeable;
import java.util.List;

public interface AisClient extends Closeable {

    void signDocumentsWithStaticCertificate(List<PdfDocument> documents);

    void signDocumentsWithOnDemandCertificate(List<PdfDocument> documents);

    void timestampDocuments(List<PdfDocument> documents);

}
