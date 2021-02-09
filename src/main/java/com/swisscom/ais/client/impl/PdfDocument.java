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
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSeedValue;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSeedValueMDP;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;

public class PdfDocument {

    private final InputStream contentIn;
    private final OutputStream contentOut;
    private final ByteArrayOutputStream inMemoryStream;
    private final String name;
    private final Trace trace;

    private String id;
    private PDDocument pdDocument;
    private ExternalSigningSupport pbSigningSupport;
    private String base64HashToSign;
    private DigestAlgorithm digestAlgorithm;

    // ----------------------------------------------------------------------------------------------------

    public PdfDocument(String name, InputStream contentIn, OutputStream contentOut, Trace trace) {
        this.name = name;
        this.contentIn = contentIn;
        this.contentOut = contentOut;
        this.inMemoryStream = new ByteArrayOutputStream();
        this.trace = trace;
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

        SignatureOptions options = new SignatureOptions();
        options.setPreferredSignatureSize(signatureType.getEstimatedSignatureSizeInBytes());

        pdDocument.addSignature(pdSignature, options);

        // Set this signature's access permissions level to 0, to ensure we just sign the PDF not certify it
        // for more details: https://wwwimages2.adobe.com/content/dam/acom/en/devnet/pdf/pdfs/PDF32000_2008.pdf see section 12.7.4.5
        // TODO what if there are more signature in the PDF?
        List<PDSignatureField> signatureFields = pdDocument.getSignatureFields();
        PDSignatureField pdSignatureField = signatureFields.get(0);

        PDSeedValue pdSeedValue = pdSignatureField.getSeedValue();
        if (pdSeedValue == null) {
            COSDictionary newSeedValueDict = new COSDictionary();
            newSeedValueDict.setNeedToBeUpdated(true);
            pdSeedValue = new PDSeedValue(newSeedValueDict);
            pdSignatureField.setSeedValue(pdSeedValue);
        }

        PDSeedValueMDP pdSeedValueMDP = pdSeedValue.getMDP();
        if (pdSeedValueMDP == null) {
            COSDictionary newMDPDict = new COSDictionary();
            newMDPDict.setNeedToBeUpdated(true);
            pdSeedValueMDP = new PDSeedValueMDP(newMDPDict);
            pdSeedValue.setMPD(pdSeedValueMDP);
        }
        pdSeedValueMDP.setP(0); // identify this signature as an author signature, not document certification

        pbSigningSupport = pdDocument.saveIncrementalForExternalSigning(inMemoryStream);

        MessageDigest digest = MessageDigest.getInstance(digestAlgorithm.getDigestAlgorithm());
        byte[] contentToSign = IOUtils.toByteArray(pbSigningSupport.getContent());
        byte[] hashToSign = digest.digest(contentToSign);
        base64HashToSign = Base64.getEncoder().encodeToString(hashToSign);
    }

    public void finishSignature(byte[] signatureContent, List<byte[]> crlEntries, List<byte[]> ocspEntries) {
        try {
            pbSigningSupport.setSignature(signatureContent);
            pdDocument.close();
            contentIn.close();
            inMemoryStream.close();

            byte[] documentBytes = inMemoryStream.toByteArray();

            if (crlEntries != null || ocspEntries != null) {
                pdDocument = PDDocument.load(documentBytes);

                CrlOcspExtender metadata = new CrlOcspExtender(pdDocument, documentBytes, signatureContent, trace);
                metadata.extendPdfWithCrlAndOcsp(crlEntries, ocspEntries);

                pdDocument.saveIncremental(contentOut);
                pdDocument.close();
            } else {
                contentOut.write(inMemoryStream.toByteArray());
            }
            contentOut.close();
        } catch (Exception e) {
            throw new AisClientException("Failed to embed the signature(s) in the document(s) and close the streams - " + trace.getId(), e);
        }
    }

    // ----------------------------------------------------------------------------------------------------

    public static void setMDPPermissionsForSignatureOnly(PDDocument doc, PDSignature signature) {
        COSDictionary transformParameters = new COSDictionary();
        transformParameters.setItem(COSName.TYPE, COSName.getPDFName("TransformParams"));
        transformParameters.setInt(COSName.P, 1); // no changes to the document shall be permitted // TODO
        transformParameters.setName(COSName.V, "1.2");
        transformParameters.setNeedToBeUpdated(true);

        COSDictionary referenceDict = new COSDictionary();
        referenceDict.setItem(COSName.TYPE, COSName.getPDFName("SigRef"));
        referenceDict.setItem("TransformMethod", COSName.DOCMDP);
        referenceDict.setItem("DigestMethod", COSName.getPDFName("SHA1"));
        referenceDict.setItem("TransformParams", transformParameters);
        referenceDict.setNeedToBeUpdated(true);

        COSArray referenceArray = new COSArray();
        referenceArray.add(referenceDict);
        referenceArray.setNeedToBeUpdated(true);

        COSDictionary sigDict = signature.getCOSObject();
        sigDict.setItem("Reference", referenceArray);

        // Catalog
        COSDictionary permsDict = new COSDictionary();
        permsDict.setItem(COSName.DOCMDP, signature);

        COSDictionary catalogDict = doc.getDocumentCatalog().getCOSObject();
        catalogDict.setItem(COSName.PERMS, permsDict);

        catalogDict.setNeedToBeUpdated(true);
        permsDict.setNeedToBeUpdated(true);
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
