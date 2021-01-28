package com.swisscom.ais.client.impl;

import com.swisscom.ais.client.AisClientException;
import com.swisscom.ais.client.model.UserData;
import com.swisscom.ais.client.rest.model.DigestAlgorithm;
import com.swisscom.ais.client.rest.model.SignatureType;
import com.swisscom.ais.client.utils.SigUtils;
import com.swisscom.ais.client.utils.Trace;
import com.swisscom.ais.client.utils.Utils;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Calendar;

public class PdfDocument {

    private final InputStream contentIn;
    private final OutputStream contentOut;
    private final String name;

    private String id;
    private PDDocument pdDocument;
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
        pdDocument = PDDocument.load(contentIn);

        int accessPermissions = SigUtils.getMDPPermission(pdDocument);
        if (accessPermissions == 1) {
            throw new AisClientException("No changes to the document are permitted due to DocMDP transform parameters dictionary");
        }

        PDSignature pdSignature = new PDSignature();
        if (signatureType == SignatureType.TIMESTAMP) {
            pdSignature.setType(COSName.DOC_TIME_STAMP);
            pdSignature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            pdSignature.setSubFilter(COSName.getPDFName("ETSI.RFC3161"));
        } else {
            pdSignature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            pdSignature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);
            // Add 3 Minutes to move signing time within the OnDemand Certificate Validity
            // This is only relevant in case the signature does not include a timestamp
            Calendar signDate = Calendar.getInstance();
            signDate.add(Calendar.MINUTE, 3);
            pdSignature.setSignDate(signDate);
        }
        pdSignature.setName(userData.getSignatureName());
        pdSignature.setReason(userData.getSignatureReason());
        pdSignature.setLocation(userData.getSignatureLocation());
        pdSignature.setContactInfo(userData.getSignatureContactInfo());

        // Optional: certify
        if (accessPermissions == 0) {
            SigUtils.setMDPPermission(pdDocument, pdSignature, 2);
        }

        SignatureOptions options = new SignatureOptions();
        options.setPreferredSignatureSize(signatureType.getEstimatedSignatureSizeInBytes() + 10000);

        pdDocument.addSignature(pdSignature, options);
        pbSigningSupport = pdDocument.saveIncrementalForExternalSigning(contentOut);

        MessageDigest digest = MessageDigest.getInstance(digestAlgorithm.getDigestAlgorithm());
        byte[] contentToSign = IOUtils.toByteArray(pbSigningSupport.getContent());
        byte[] hashToSign = digest.digest(contentToSign);
        base64HashToSign = Base64.getEncoder().encodeToString(hashToSign);
    }

    public void finishSignature(byte[] signatureContent, Trace trace) {
        try {
            pbSigningSupport.setSignature(signatureContent);
            pdDocument.close();
            contentIn.close();
            contentOut.close();
            // no support yet for embedding OCSP and CRL in the final PDF
        } catch (Exception e) {
            throw new AisClientException("Failed to embed the signature(s) in the document(s) and close the streams - " + trace.getId(), e);
        }
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

}
