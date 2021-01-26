package com.swisscom.ais.client.impl;

import com.swisscom.ais.client.AisClientException;
import com.swisscom.ais.client.model.UserData;
import com.swisscom.ais.client.rest.model.DigestAlgorithm;
import com.swisscom.ais.client.rest.model.SignatureType;
import com.swisscom.ais.client.utils.SigUtils;
import com.swisscom.ais.client.utils.Trace;
import com.swisscom.ais.client.utils.Utils;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;

public class PdfDocument {

    private final InputStream contentIn;
    private final OutputStream contentOut;
    private final String name;

    private String id;
    private PDDocument pbDocument;
    private ExternalSigningSupport pbSigningSupport;
    private String base64HashToSign;
    private DigestAlgorithm digestAlgorithm;

    // ----------------------------------------------------------------------------------------------------

    public PdfDocument(String name, InputStream contentIn, OutputStream contentOut) {
        this.name = name;
        this.contentIn = contentIn;
        this.contentOut = contentOut;
    }

    public void prepareForSigning(DigestAlgorithm digestAlgorithm,
                                  SignatureType signatureType,
                                  UserData userData) throws IOException, NoSuchAlgorithmException {
        this.digestAlgorithm = digestAlgorithm;
        id = Utils.generateDocumentId();
        pbDocument = PDDocument.load(contentIn);

        int accessPermissions = SigUtils.getMDPPermission(pbDocument);
        if (accessPermissions == 1) {
            throw new AisClientException("No changes to the document are permitted due to DocMDP transform parameters dictionary");
        }

        PDSignature signature = new PDSignature();
        if (signatureType == SignatureType.TIMESTAMP) {
            signature.setType(COSName.DOC_TIME_STAMP);
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(COSName.getPDFName("ETSI.RFC3161"));
        } else {
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);
            // Add 3 Minutes to move signing time within the OnDemand Certificate Validity
            // This is only relevant in case the signature does not include a timestamp
            Calendar signDate = Calendar.getInstance();
            signDate.add(Calendar.MINUTE, 3);
            signature.setSignDate(signDate);
        }
        signature.setName(userData.getSignatureName());
        signature.setReason(userData.getSignatureReason());
        signature.setLocation(userData.getSignatureLocation());
        signature.setContactInfo(userData.getSignatureContactInfo());

        // Optional: certify
        if (accessPermissions == 0) {
            SigUtils.setMDPPermission(pbDocument, signature, 2);
        }

        SignatureOptions options = new SignatureOptions();
        options.setPreferredSignatureSize(signatureType.getEstimatedSignatureSizeInBytes());

        pbDocument.addSignature(signature, options);
        pbSigningSupport = pbDocument.saveIncrementalForExternalSigning(contentOut);

        MessageDigest digest = MessageDigest.getInstance(digestAlgorithm.getDigestAlgorithm());
        byte[] contentToSign = IOUtils.toByteArray(pbSigningSupport.getContent());
        byte[] hashToSign = digest.digest(contentToSign);
        base64HashToSign = Base64.getEncoder().encodeToString(hashToSign);
    }

    public void finishSignature(byte[] signature, byte[] crlContent, byte[] ocspContent, Trace trace) {
        try {
            pbSigningSupport.setSignature(signature);
            PDDocumentCatalog documentCatalog = pbDocument.getDocumentCatalog();
            COSDictionary catalogDictionary = documentCatalog.getCOSObject();
            catalogDictionary.setNeedToBeUpdated(true);
            COSDictionary dss = createDssDictionary(null,
                                                    Arrays.asList(crlContent),
                                                    Arrays.asList(ocspContent));
            addExtensions(documentCatalog);
            catalogDictionary.setItem(COSName.getPDFName("DSS"), dss);
            pbDocument.saveIncremental(contentOut);
            closeDocument();
        } catch (Exception e) {
            throw new AisClientException("Failed to embed the signature(s) in the document(s) and close the streams - " + trace.getId(), e);
        }
    }

    // ----------------------------------------------------------------------------------------------------

    COSDictionary createDssDictionary(Iterable<byte[]> certificates, Iterable<byte[]> crls, Iterable<byte[]> ocspResponses) throws IOException {
        final COSDictionary dssDictionary = new COSDictionary();
        dssDictionary.setNeedToBeUpdated(true);
        dssDictionary.setName(COSName.TYPE, "DSS");

        if (certificates != null) {
            dssDictionary.setItem(COSName.getPDFName("Certs"), createArray(certificates));
        }
        if (crls != null) {
            dssDictionary.setItem(COSName.getPDFName("CRLs"), createArray(crls));
        }
        if (ocspResponses != null) {
            dssDictionary.setItem(COSName.getPDFName("OCSPs"), createArray(ocspResponses));
        }

        return dssDictionary;
    }

    COSArray createArray(Iterable<byte[]> datas) throws IOException {
        COSArray array = new COSArray();
        array.setNeedToBeUpdated(true);

        if (datas != null) {
            for (byte[] data : datas) {
                array.add(createStream(data));
            }
        }

        return array;
    }

    COSStream createStream(byte[] data) throws IOException {
        COSStream stream = pbDocument.getDocument().createCOSStream();
        stream.setNeedToBeUpdated(true);
        OutputStream outputStream = stream.createOutputStream();
        outputStream.write(data);
        outputStream.flush();
        outputStream.close();
        return stream;
    }

    /**
     * Adds Extensions to the document catalog. So that the use of DSS is identified. Described in
     * PAdES Part 4, Chapter 4.4.
     *
     * @param catalog to add Extensions into
     */
    private void addExtensions(PDDocumentCatalog catalog) {
        COSDictionary dssExtensions = new COSDictionary();
        dssExtensions.setDirect(true);
        catalog.getCOSObject().setItem("Extensions", dssExtensions);

        COSDictionary adbeExtension = new COSDictionary();
        adbeExtension.setDirect(true);
        dssExtensions.setItem("ADBE", adbeExtension);

        adbeExtension.setName("BaseVersion", "1.7");
        adbeExtension.setInt("ExtensionLevel", 5);

        catalog.setVersion("1.7");
    }

    // ----------------------------------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBase64HashToSign() {
        return base64HashToSign;
    }

    public DigestAlgorithm getDigestAlgorithm() {
        return digestAlgorithm;
    }

    // ----------------------------------------------------------------------------------------------------

    private void closeDocument() throws IOException {
        pbDocument.close();
        contentIn.close();
        contentOut.close();
    }

}
