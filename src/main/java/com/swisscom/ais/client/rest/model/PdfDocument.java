package com.swisscom.ais.client.rest.model;

import com.swisscom.ais.client.AisClientException;
import com.swisscom.ais.client.utils.Utils;
import com.swisscom.ais.pdfbox.SigUtils;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Calendar;

public class PdfDocument implements Closeable {

    private final InputStream contentIn;
    private final OutputStream contentOut;

    private String id;

    private String name;

    private PDDocument pbDocument;

    private PDSignature pbSignature;

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
                                  SignatureType signatureType) throws IOException, NoSuchAlgorithmException {
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
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            // TODO the iText samples were setting the time of the signatures 3 minutes in the future
            // Add 3 Minutes to move signing time within the OnDemand Certificate Validity
            // This is only relevant in case the signature does not include a timestamp
            Calendar signDate = Calendar.getInstance();
            signDate.add(Calendar.MINUTE, 3);
            signature.setSignDate(signDate);
        }

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

    @Override
    public void close() throws IOException {
        pbDocument.close();
        contentIn.close();
        contentOut.close();
    }

    // ----------------------------------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PDDocument getPbDocument() {
        return pbDocument;
    }

    public PDSignature getPbSignature() {
        return pbSignature;
    }

    public ExternalSigningSupport getPbSigningSupport() {
        return pbSigningSupport;
    }

    public String getBase64HashToSign() {
        return base64HashToSign;
    }

    public DigestAlgorithm getDigestAlgorithm() {
        return digestAlgorithm;
    }
}
