package com.swisscom.ais.client;

import com.swisscom.ais.client.model.PdfDocument;
import com.swisscom.ais.client.rest.ModelBuilder;
import com.swisscom.ais.client.rest.RestClient;
import com.swisscom.ais.client.rest.model.signreq.AISSignRequest;
import com.swisscom.ais.client.rest.model.signresp.AISSignResponse;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

public class AisClientImpl implements AisClient {

    private RestClient restClient;

    @Override
    public void timestampDocuments(List<PdfDocument> documents) {
        PdfDocument document = documents.get(0);
        try {
            FileInputStream fileIn = new FileInputStream(document.getInputFromFile());
            FileOutputStream fileOut = new FileOutputStream(document.getOutputToFile());
            PDDocument doc = PDDocument.load(fileIn);

            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            //ExternalSigningSupport externalSigning = doc.saveIncrementalForExternalSigning(fileOut);

            PDSignature signature = new PDSignature();
            signature.setType(COSName.DOC_TIME_STAMP);
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(COSName.getPDFName("ETSI.RFC3161"));

            SignatureOptions options = new SignatureOptions();
            options.setPreferredSignatureSize(15000);

            doc.addSignature(signature, options);
            ExternalSigningSupport externalSigning = doc.saveIncrementalForExternalSigning(fileOut);

            byte[] contentToSign = IOUtils.toByteArray(externalSigning.getContent());
            byte[] hashToSign = digest.digest(contentToSign);
            String base64HashToSign = Base64.getEncoder().encodeToString(hashToSign);

            // do the thing, get the timestamp token
            AISSignRequest signRequest = ModelBuilder.buildAisSignRequest(
                "http://www.w3.org/2001/04/xmlenc#sha512",
                base64HashToSign,
                CoreValues.TIMESTAMP_TYPE_RFC_3161);
            AISSignResponse signResponse = restClient.requestSignature(signRequest);
            if (!CoreValues.responseIsMajorSuccess(signResponse)) {
                // TODO
                System.out.println("ERROR. Cannot timestamp document");
                System.exit(1);
            }

            String base64TimestampToken = signResponse.getSignResponse().getSignatureObject().getTimestamp().getRFC3161TimeStampToken();
            externalSigning.setSignature(Base64.getDecoder().decode(base64TimestampToken));

            // signature.setContents(timestampToken);

            // No certification allowed because /Reference not allowed in signature directory
            // see ETSI EN 319 142-1 Part 1 and ETSI TS 102 778-4
            // http://www.etsi.org/deliver/etsi_en%5C319100_319199%5C31914201%5C01.01.00_30%5Cen_31914201v010100v.pdf
            // http://www.etsi.org/deliver/etsi_ts/102700_102799/10277804/01.01.01_60/ts_10277804v010101p.pdf

            // register signature dictionary and sign interface
//            doc.addSignature(signature, new SignatureInterface() {
//                @Override
//                public byte[] sign(InputStream content) throws IOException {
//                    byte[] contentToSign = IOUtils.toByteArray(content);
//                    byte[] hashToSign = digest.digest(contentToSign);
//                    String base64HashToSign = Base64.getEncoder().encodeToString(hashToSign);
//
//                    // do the thing, get the timestamp token
//                    AISSignRequest signRequest = ModelBuilder.buildAisSignRequest(
//                        "http://www.w3.org/2001/04/xmlenc#sha512",
//                        base64HashToSign,
//                        CoreValues.TIMESTAMP_TYPE_RFC_3161);
//                    AISSignResponse signResponse = restClient.requestSignature(signRequest);
//                    if (!CoreValues.responseIsMajorSuccess(signResponse)) {
//                        // TODO
//                        System.out.println("ERROR. Cannot timestamp document");
//                        System.exit(1);
//                    }
//
//                    String base64TimestampToken = signResponse.getSignResponse().getSignatureObject().getTimestamp().getRFC3161TimeStampToken();
//                    return Base64.getDecoder().decode(base64TimestampToken);
//                }
//            });

            // write incremental (only for signing purpose)
            // doc.saveIncremental(fileOut);
            // close everything
            doc.close();
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
