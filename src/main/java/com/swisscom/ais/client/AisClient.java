package com.swisscom.ais.client;

import com.swisscom.ais.client.model.PdfDocument;

import java.io.Closeable;
import java.util.List;

public interface AisClient extends Closeable {

    void timestampDocuments(List<PdfDocument> documents);

}
