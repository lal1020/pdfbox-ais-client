/*
 * Copyright 2021 Swisscom Trust Services (Schweiz) AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.swisscom.ais.client.impl;

import com.swisscom.ais.client.AisClientException;
import com.swisscom.ais.client.rest.model.SignatureType;
import com.swisscom.ais.client.utils.Loggers;
import com.swisscom.ais.client.utils.Trace;
import com.swisscom.ais.client.utils.Utils;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.util.*;
import java.util.stream.Collectors;

public class CrlOcspExtender {

    private static final Logger logPdfProcessing = LoggerFactory.getLogger(Loggers.PDF_PROCESSING);

    private static final String DICTIONARY_DSS = "DSS";
    private static final COSName COSNAME_DSS;
    private static final COSName COSNAME_OCSPS;
    private static final COSName COSNAME_CRLS;
    private static final COSName COSNAME_CERTS;
    private static final COSName COSNAME_TYPE;

    static {
        COSNAME_DSS = COSName.getPDFName("DSS");
        COSNAME_OCSPS = COSName.getPDFName("OCSPs");
        COSNAME_CRLS = COSName.getPDFName("CRLs");
        COSNAME_CERTS = COSName.getPDFName("Certs");
        COSNAME_TYPE = COSName.TYPE;
    }

    // ----------------------------------------------------------------------------------------------------

    private final Trace trace;
    private final PDDocument pdDocument;
    private final COSDocument cosDocument;
    private final byte[] documentBytes;

    public CrlOcspExtender(PDDocument pdDocument, byte[] documentBytes, Trace trace) {
        this.pdDocument = pdDocument;
        this.documentBytes = documentBytes;
        this.cosDocument = pdDocument.getDocument();
        this.trace = trace;
    }

    public void extendPdfWithCrlAndOcsp(List<byte[]> crlEntries, List<byte[]> ocspEntries) {
        try {
            PDDocumentCatalog pdDocumentCatalog = pdDocument.getDocumentCatalog();
            COSDictionary cosDocumentCatalog = pdDocumentCatalog.getCOSObject();
            cosDocumentCatalog.setNeedToBeUpdated(true);

            addExtensions(pdDocumentCatalog);

            List<byte[]> encodedCrlEntries = getCrlEncodedForm(crlEntries);
            List<byte[]> encodedOcspEntries = getOcspEncodedForm(ocspEntries);

            Map<COSName, ValidationData> validationMap = new HashMap<>();

            PDSignature lastSignature = getLastRelevantSignature(pdDocument);
            if (lastSignature == null) {
                throw new AisClientException("Cannot extend PDF with CRL and OCSP data. No signature was found in the PDF");
            }

            ValidationData vData = new ValidationData();
            for (byte[] ocsp : encodedOcspEntries) {
                vData.ocsps.add(buildOCSPResponse(ocsp));
            }
            vData.crls.addAll(encodedCrlEntries);
            validationMap.put(COSName.getPDFName(getSignatureHashKey(lastSignature)), vData);

            // ----------------------------------------------------------------------------------------------------
            COSDictionary pdDssDict = cosDocumentCatalog.getCOSDictionary(COSNAME_DSS);
            COSArray ocsps = null;
            COSArray crls = null;
            COSArray certs = null;

            if (pdDssDict != null) {
                ocsps = pdDssDict.getCOSArray(COSNAME_OCSPS);
                crls = pdDssDict.getCOSArray(COSNAME_CRLS);
                certs = pdDssDict.getCOSArray(COSNAME_CERTS);
                pdDssDict.removeItem(COSNAME_OCSPS);
                pdDssDict.removeItem(COSNAME_CRLS);
                pdDssDict.removeItem(COSNAME_CERTS);
            }
            pdDssDict = (pdDssDict != null) ? pdDssDict : createDssDictionary();
            ocsps = (ocsps != null) ? ocsps : createArray();
            crls = (crls != null) ? crls : createArray();
            certs = (certs != null) ? certs : createArray();

            for (Map.Entry<COSName, ValidationData> validationEntry : validationMap.entrySet()) {
                ValidationData validationData = validationEntry.getValue();
                for (byte[] ocspBytes : validationData.ocsps) {
                    COSStream stream = createStream(ocspBytes, cosDocument);
                    ocsps.add(stream);
                }
                for (byte[] crlBytes : validationData.crls) {
                    COSStream stream = createStream(crlBytes, cosDocument);
                    crls.add(stream);
                }
                for (byte[] certBytes : validationData.certs) {
                    COSStream stream = createStream(certBytes, cosDocument);
                    certs.add(stream);
                }
            }

            if (ocsps.size() > 0) {
                pdDssDict.setItem(COSNAME_OCSPS, ocsps);
            }
            if (crls.size() > 0) {
                pdDssDict.setItem(COSNAME_CRLS, crls);
            }
            if (certs.size() > 0) {
                pdDssDict.setItem(COSNAME_CERTS, certs);
            }

            cosDocumentCatalog.setItem(COSNAME_DSS, pdDssDict);
        } catch (Exception e) {
            throw new AisClientException("An error occurred processing the signature and embedding CRL and OCSP data", e);
        }
    }

    // ----------------------------------------------------------------------------------------------------

    private PDSignature getLastRelevantSignature(PDDocument document) throws IOException {
        SortedMap<Integer, PDSignature> sortedMap = new TreeMap<>();
        for (PDSignature signature : document.getSignatureDictionaries()) {
            int sigOffset = signature.getByteRange()[1];
            sortedMap.put(sigOffset, signature);
        }
        if (sortedMap.size() > 0) {
            PDSignature lastSignature = sortedMap.get(sortedMap.lastKey());
            COSBase type = lastSignature.getCOSObject().getItem(COSName.TYPE);
            if (type.equals(COSName.SIG) || type.equals(COSName.DOC_TIME_STAMP)) {
                return lastSignature;
            }
        }
        return null;
    }

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

    private List<byte[]> getCrlEncodedForm(List<byte[]> crlEntries) {
        if (crlEntries == null) {
            return Collections.emptyList();
        }
        return crlEntries.stream().map(crl -> {
            try {
                X509CRL x509crl = (X509CRL) CertificateFactory.getInstance("X.509").generateCRL(new ByteArrayInputStream(crl));
                if (logPdfProcessing.isDebugEnabled()) {
                    String message = "\nEmbedding CRL..."
                                     + "\nIssuer DN                   : " + x509crl.getIssuerDN()
                                     + "\nThis update                 : " + x509crl.getThisUpdate()
                                     + "\nNext update                 : " + x509crl.getNextUpdate()
                                     + "\nNo. of revoked certificates : " + ((x509crl.getRevokedCertificates() == null) ?
                                                                             "0" : x509crl.getRevokedCertificates().size());
                    logPdfProcessing.debug(message + " - " + trace.getId());
                }
                return x509crl.getEncoded();
            } catch (Exception e) {
                throw new AisClientException("Failed to generate X509CRL from CRL content received from AIS", e);
            }
        }).collect(Collectors.toList());
    }

    private List<byte[]> getOcspEncodedForm(List<byte[]> ocspEntries) {
        if (ocspEntries == null) {
            return Collections.emptyList();
        }
        return ocspEntries.stream().map(ocsp -> {
            try {
                OCSPResp ocspResp = new OCSPResp(new ByteArrayInputStream(ocsp));
                BasicOCSPResp basicResp = (BasicOCSPResp) ocspResp.getResponseObject();
                if (logPdfProcessing.isDebugEnabled()) {
                    String certificateId = basicResp.getResponses()[0].getCertID().getSerialNumber().toString() + " (" +
                                           basicResp.getResponses()[0].getCertID().getSerialNumber().toString(16).toUpperCase() + ")";
                    String message = "\nEmbedding OCSP Response..."
                                     + "\nStatus                : " + ((ocspResp.getStatus() == 0) ? "GOOD" : "BAD")
                                     + "\nProduced at           : " + basicResp.getProducedAt()
                                     + "\nThis update           : " + basicResp.getResponses()[0].getThisUpdate()
                                     + "\nNext update           : " + basicResp.getResponses()[0].getNextUpdate()
                                     + "\nX509 Cert issuer      : " + basicResp.getCerts()[0].getIssuer()
                                     + "\nX509 Cert subject     : " + basicResp.getCerts()[0].getSubject()
                                     + "\nCertificate ID        : " + certificateId;
                    logPdfProcessing.debug(message + " - " + trace.getId());
                }
                return basicResp.getEncoded(); // Add Basic OCSP Response to Collection (ASN.1 encoded representation of this object)
            } catch (Exception e) {
                throw new AisClientException("Failed to generate X509CRL from CRL content received from AIS", e);
            }
        }).collect(Collectors.toList());

    }

    private COSDictionary createDssDictionary() {
        COSDictionary dictionary = new COSDictionary();
        dictionary.setNeedToBeUpdated(true);
        dictionary.setName(COSNAME_TYPE, DICTIONARY_DSS);
        dictionary.setDirect(true);
        return dictionary;
    }

    private COSArray createArray() {
        COSArray array = new COSArray();
        array.setNeedToBeUpdated(true);
        return array;
    }

    private COSStream createStream(byte[] data, COSDocument cosDocument) throws IOException {
        COSStream stream = cosDocument.createCOSStream();
        stream.setNeedToBeUpdated(true);
        try (OutputStream unfilteredStream = stream.createOutputStream(COSName.FLATE_DECODE)) {
            unfilteredStream.write(data);
            unfilteredStream.flush();
        }
        return stream;
    }

    private byte[] buildOCSPResponse(byte[] content) throws IOException {
        DEROctetString derOctet = new DEROctetString(content);
        ASN1EncodableVector v2 = new ASN1EncodableVector();
        v2.add(OCSPObjectIdentifiers.id_pkix_ocsp_basic);
        v2.add(derOctet);
        ASN1Enumerated den = new ASN1Enumerated(0);
        ASN1EncodableVector v3 = new ASN1EncodableVector();
        v3.add(den);
        v3.add(new DERTaggedObject(true, 0, new DERSequence(v2)));
        DERSequence seq = new DERSequence(v3);
        return seq.getEncoded();
    }

    private String getSignatureHashKey(PDSignature signature) throws NoSuchAlgorithmException, IOException {
        byte[] contentToConvert = signature.getContents(documentBytes);
        if (SignatureType.TIMESTAMP.getUri().equals(signature.getSubFilter())) {
            ASN1InputStream din = new ASN1InputStream(new ByteArrayInputStream(contentToConvert));
            ASN1Primitive pkcs = din.readObject();
            contentToConvert = pkcs.getEncoded();
        }
        return Utils.convertToHexString(Utils.hashBytesWithSha1(contentToConvert));
    }

    // ----------------------------------------------------------------------------------------------------

    private static class ValidationData {
        public List<byte[]> crls = new ArrayList<>();
        public List<byte[]> ocsps = new ArrayList<>();
        public List<byte[]> certs = new ArrayList<>();
    }

}
