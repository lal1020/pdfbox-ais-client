package com.swisscom.ais.client;

import com.swisscom.ais.client.model.PdfHandle;
import com.swisscom.ais.client.rest.RestClient;
import com.swisscom.ais.client.rest.model.DigestAlgorithm;
import com.swisscom.ais.client.rest.model.ModelBuilder;
import com.swisscom.ais.client.rest.model.PdfDocument;
import com.swisscom.ais.client.rest.model.signreq.AISSignRequest;
import com.swisscom.ais.client.rest.model.signresp.AISSignResponse;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class AisClientImpl implements AisClient {

    private RestClient restClient;

    // ----------------------------------------------------------------------------------------------------

    @Override
    public void signDocumentsWithStaticCertificate(List<PdfHandle> documents) {
        // TODO
    }

    @Override
    public void signDocumentsWithOnDemandCertificate(List<PdfHandle> documents) {
        // TODO
    }

    @Override
    public void timestampDocuments(List<PdfHandle> documentHandles) {
        List<PdfDocument> documentsToSign = new ArrayList<>(documentHandles.size());
        try {
            for (PdfHandle handle : documentHandles) {
                FileInputStream fileIn = new FileInputStream(handle.getInputFromFile());
                FileOutputStream fileOut = new FileOutputStream(handle.getOutputToFile());

                PdfDocument newDocument = new PdfDocument(fileIn, fileOut);
                newDocument.prepareForSigning(DigestAlgorithm.SHA512);

                documentsToSign.add(newDocument);
            }
        } catch (Exception e) {
            // TODO
            e.printStackTrace();
        }

        try {

            // do the thing, get the timestamp token
            AISSignRequest signRequest = ModelBuilder.buildAisSignRequest(
                documentsToSign,
                CoreValues.TIMESTAMP_TYPE_RFC_3161);
            AISSignResponse signResponse = restClient.requestSignature(signRequest);
            if (!CoreValues.responseIsMajorSuccess(signResponse)) {
                // TODO
                System.out.println("ERROR. Cannot timestamp document");
                System.exit(1);
            }

            String base64TimestampToken = signResponse.getSignResponse().getSignatureObject().getTimestamp().getRFC3161TimeStampToken();
            externalSigning.setSignature(Base64.getDecoder().decode(base64TimestampToken));

            // close everything
            doc.close();
            fileIn.close();
            fileOut.close();
        } catch (Exception e) {
            // TODO
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    @Override
    public void close() throws IOException {
        if (restClient != null) {
            restClient.close();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

}
