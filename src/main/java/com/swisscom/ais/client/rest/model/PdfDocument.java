package com.swisscom.ais.client.rest.model;

import com.swisscom.ais.client.utils.Utils;

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

public class PdfDocument implements Closeable {

    private String id;

    private InputStream contentIn;

    private OutputStream contentOut;

    private PDDocument pbDocument;

    private PDSignature pbSignature;

    private ExternalSigningSupport pbSigningSupport;

    private String base64HashToSign;

    private DigestAlgorithm digestAlgorithm;

    // ----------------------------------------------------------------------------------------------------

    public PdfDocument(InputStream contentIn, OutputStream contentOut) {
        this.contentIn = contentIn;
        this.contentOut = contentOut;
    }

    public void prepareForSigning(DigestAlgorithm digestAlgorithm) throws IOException, NoSuchAlgorithmException {
        this.digestAlgorithm = digestAlgorithm;
        id = Utils.generateDocumentId();
        pbDocument = PDDocument.load(contentIn);

        PDSignature signature = new PDSignature();
        signature.setType(COSName.DOC_TIME_STAMP);
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(COSName.getPDFName("ETSI.RFC3161"));

        SignatureOptions options = new SignatureOptions();
        options.setPreferredSignatureSize(15000);

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
