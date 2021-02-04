package com.swisscom.ais.client.impl;

import com.swisscom.ais.client.AisClientException;
import com.swisscom.ais.client.utils.Loggers;
import com.swisscom.ais.client.utils.Trace;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CrlOcspExtender {

    private static final Logger logPdfProcessing = LoggerFactory.getLogger(Loggers.PDF_PROCESSING);

    private final Trace trace;
    private final PDDocument pdDocument;
    private final byte[] signatureContent;

    public CrlOcspExtender(PDDocument pdDocument, byte[] signatureContent, Trace trace) {
        this.pdDocument = pdDocument;
        this.signatureContent = signatureContent;
        this.trace = trace;
    }

    public void extendPdfWithCrlAndOcsp(List<byte[]> crlEntries, List<byte[]> ocspEntries) {
        try {
            COSDictionary catalogDict = pdDocument.getDocumentCatalog().getCOSObject();
            catalogDict.setNeedToBeUpdated(true);

            CMSSignedData data = new CMSSignedData(signatureContent);
            Store<X509CertificateHolder> certificatesStore = data.getCertificates();

            // prepare certificates for embedding
            List<byte[]> encodedCerts = new ArrayList<>(certificatesStore.getMatches(null)).stream().map(certHolder -> {
                X509Certificate x509cert;
                try {
                    x509cert = new JcaX509CertificateConverter().getCertificate(certHolder);
                    if (logPdfProcessing.isDebugEnabled()) {
                        String message = "\nEmbedding certificate..."
                                         + "\nSubject DN                   : " + x509cert.getSubjectDN()
                                         + "\nIssuer DN                    : " + x509cert.getIssuerDN()
                                         + "\nValid not before             : " + x509cert.getNotBefore()
                                         + "\nValid not after              : " + x509cert.getNotAfter();
                        logPdfProcessing.debug(message + " - " + trace.getId());
                    }
                    return x509cert.getEncoded();
                } catch (CertificateException e) {
                    throw new AisClientException("Failed to generate encoded X509 Certificate for the received signature", e);
                }
            }).collect(Collectors.toList());

            // prepare CRLs for embedding
            List<byte[]> encodedCrls = crlEntries.stream().map(crl -> {
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

            // prepare OCSP data for embedding
            List<byte[]> encodedOcsps = ocspEntries.stream().map(ocsp -> {
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

            COSDictionary dss = createDssDictionary(encodedCerts, encodedCrls, encodedOcsps);
            catalogDict.setItem(COSName.getPDFName("DSS"), dss);
        } catch (Exception e) {
            throw new AisClientException("An error occurred processing the signature and embedding CRL and OCSP data", e);
        }
    }

    // ----------------------------------------------------------------------------------------------------

    public COSDictionary createDssDictionary(Iterable<byte[]> certificates, Iterable<byte[]> crls, Iterable<byte[]> ocspResponses)
        throws IOException {
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


    public COSArray createArray(Iterable<byte[]> datas) throws IOException {
        COSArray array = new COSArray();
        array.setNeedToBeUpdated(true);

        if (datas != null) {
            for (byte[] data : datas) {
                array.add(createStream(data));
            }
        }

        return array;
    }

    public COSStream createStream(byte[] data) throws IOException {
        COSStream stream = new COSStream();
        stream.setNeedToBeUpdated(true);
        final OutputStream unfilteredStream = stream.createOutputStream(COSName.FLATE_DECODE);
        unfilteredStream.write(data);
        unfilteredStream.flush();
        unfilteredStream.close();
        return stream;
    }

}
