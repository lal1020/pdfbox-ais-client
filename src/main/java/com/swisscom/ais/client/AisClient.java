package com.swisscom.ais.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swisscom.ais.client.rest.model.signreq.AISSignRequest;
import com.swisscom.ais.client.rest.model.signreq.DocumentHash;
import com.swisscom.ais.client.rest.model.signreq.DsigDigestMethod;
import com.swisscom.ais.client.rest.model.signreq.InputDocuments;
import com.swisscom.ais.client.rest.model.signreq.SignRequest;
import com.swisscom.ais.client.rest.model.signresp.AISSignResponse;
import com.swisscom.ais.client.utils.Utils;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.binary.Base64;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.PrivateKeyStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

public class AisClient implements Closeable {

    private static final String CLIENT_KEY_FILE = "D:/Projects/Swisscom/AIS/Tests/bogdan-mocanu-ais.key";
    private static final String CLIENT_CERTIFICATE_FILE = "D:/Projects/Swisscom/AIS/Tests/bogdan-mocanu-ais.crt";
    private static final String SERVER_CERTIFICATE_FILE = "D:/Projects/Swisscom/AIS/Tests/ais-ca-ssl.crt";
    private static final String CLIENT_KEYSTORE_PASSWORD = "secret"; // built-in
    private static final String CLIENT_KEYSTORE_KEY_PASSWORD = "secret";

    private static final int CLIENT_MAX_CONNECTION_TOTAL = 20;
    private static final int CLIENT_MAX_CONNECTIONS_PER_ROUTE = 10;
    private static final int CLIENT_SOCKET_TIMEOUT_IN_MS = 10000; // 10s
    private static final int CLIENT_RESPONSE_TIMEOUT_IN_MS = 20000; // 20s

    // ----------------------------------------------------------------------------------------------------

    public static void main(String[] args) {
        AisClient client = new AisClient();
        client.initialize();

    }

    // ----------------------------------------------------------------------------------------------------

    private static final Logger log = LoggerFactory.getLogger(AisClient.class);

    private ObjectMapper jacksonMapper;
    private CloseableHttpClient httpClient;

    public void initialize() {
        jacksonMapper = new ObjectMapper();

        SSLConnectionSocketFactory sslConnectionSocketFactory;
        try {
            SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                .loadKeyMaterial(produceTheKeyStore(), CLIENT_KEYSTORE_KEY_PASSWORD.toCharArray(), produceAPrivateKeyStrategy());
            sslContextBuilder.loadTrustMaterial(produceTheTrustStore(), null);
            sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure the TLS/SSL connection factory for the AIS client", e);
        }

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(CLIENT_MAX_CONNECTION_TOTAL)
            .setMaxConnPerRoute(CLIENT_MAX_CONNECTIONS_PER_ROUTE)
            .setSSLSocketFactory(sslConnectionSocketFactory)
            .build();
        RequestConfig httpClientRequestConfig = RequestConfig.custom()
            .setConnectTimeout(CLIENT_SOCKET_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS)
            .setResponseTimeout(CLIENT_RESPONSE_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS)
            .build();

        httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(httpClientRequestConfig)
            .build();
    }

    @Override
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    public void requestSignature() {
        // TODO this only allows one input document to be specified
        DocumentHash documentHash = new DocumentHash();
        documentHash.setId(Utils.generateDocumentId());
        documentHash.setDsigDigestMethod(new DsigDigestMethod().withAlgorithm(DefaultValues.DIGEST_ALGO_SHA512));
        // TODO change this to actual digest
        documentHash.setDsigDigestValue("NA6wKClPbA+TYHW7GhPIiXn6gCGv9gSqOa508QzLGeJJYLjOfVD1tSD820M8btjEP49VhiAVK9xc/Y1z6hx+6g==");

        InputDocuments inputDocuments = new InputDocuments();
        inputDocuments.setDocumentHash(documentHash);

        SignRequest request = new SignRequest();
        request.setRequestID(Utils.generateRequestId());
        request.setProfile(DefaultValues.SWISSCOM_BASIC_PROFILE);

        AISSignRequest requestWrapper = new AISSignRequest();
        requestWrapper.setSignRequest(request);

        AISSignResponse responseWrapper = sendAndReceive("SignRequest", DefaultValues.AIS_REST_URL,
                                                         requestWrapper, AISSignResponse.class);

    }

    // ----------------------------------------------------------------------------------------------------

    private <TReq, TResp> TResp sendAndReceive(String operationName,
                                               String serviceUrl,
                                               TReq requestObject,
                                               Class<TResp> responseClass) {
        log.debug("{}: Serializing object of type {} to JSON", operationName, requestObject.getClass().getSimpleName());
        String requestJson;
        try {
            requestJson = jacksonMapper.writeValueAsString(requestObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request object to JSON, for operation " + operationName, e);
        }

        HttpPost httpPost = new HttpPost(serviceUrl);
        httpPost.setEntity(new StringEntity(requestJson, ContentType.APPLICATION_JSON, CharEncoding.UTF_8, false));
        log.info("{}: Sending request to: [{}]", operationName, serviceUrl);
        log.debug("{}: Sending JSON to: [{}], content: [{}]", operationName, serviceUrl, requestJson);

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            log.info("{}: Received HTTP status code: {}", operationName, response.getCode());
            String responseJson;
            try {
                responseJson = EntityUtils.toString(response.getEntity());
            } catch (ParseException e) {
                throw new RuntimeException("Failed to interpret the HTTP response content as a string, for operation " + operationName, e);
            }
            if (response.getCode() == 200) {
                log.debug("{}: Received JSON content: {}", operationName, responseJson);
                log.debug("{}: Deserializing JSON to object of type {}", operationName, responseClass.getSimpleName());
                try {
                    return jacksonMapper.readValue(responseJson, responseClass);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to deserialize JSON content to object of type " +
                                               responseClass.getSimpleName() + " for operation " + operationName, e);
                }
            } else {
                throw new RuntimeException("Received fault response");
//                logProtocol.debug("{}: Deserializing JSON to object of type {}", operationName, MSSFault.class.getSimpleName());
//                try {
//                    faultWrapper = jacksonMapper.readValue(responseJson, MSSFault.class);
//                } catch (JsonProcessingException e) {
//                    throw new MIDFlowException("Failed to deserialize JSON content to object of type " +
//                                               MSSFault.class.getSimpleName() + " for operation " + operationName,
//                                               e, faultProcessor.processException(e, FailureReason.RESPONSE_PARSING_FAILURE));
//                }
            }
        } catch (SSLException e) {
            throw new RuntimeException("TLS/SSL connection failure for " + operationName, e);
        } catch (Exception e) {
            throw new RuntimeException("Communication failure for " + operationName, e);
        }
    }

    // ----------------------------------------------------------------------------------------------------

    private KeyStore produceTheKeyStore() {
        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            FileInputStream is = new FileInputStream(CLIENT_CERTIFICATE_FILE);
            X509Certificate certificate = (X509Certificate) fact.generateCertificate(is);
            RSAPrivateKey privateKey = getPrivateKey(CLIENT_KEY_FILE);

            KeyStore keyStore = KeyStore.getInstance("jks");
            keyStore.load(null, null);
            keyStore.setKeyEntry("main", privateKey, CLIENT_KEYSTORE_PASSWORD.toCharArray(), new Certificate[]{certificate});

            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize the TLS keystore", e);
        }
    }

    private KeyStore produceTheTrustStore() {
        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            FileInputStream is = new FileInputStream(SERVER_CERTIFICATE_FILE);
            X509Certificate certificate = (X509Certificate) fact.generateCertificate(is);

            KeyStore keyStore = KeyStore.getInstance("jks");
            keyStore.load(null, null);
            keyStore.setCertificateEntry("main", certificate);

            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize the TLS truststore", e);
        }
    }

    private static String getKey(String filename) throws IOException {
        // Read key from file
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder contentBuilder = new StringBuilder(500);
        String line;
        while ((line = br.readLine()) != null) {
            contentBuilder.append(line).append('\n');
        }
        br.close();
        return contentBuilder.toString();
    }

    public static RSAPrivateKey getPrivateKey(String filename) throws IOException, GeneralSecurityException {
        String privateKeyPEM = getKey(filename);
        privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----\n", "");
        privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "");
        byte[] encoded = Base64.decodeBase64(privateKeyPEM);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) kf.generatePrivate(keySpec);
    }

    private PrivateKeyStrategy produceAPrivateKeyStrategy() {
        return (aliases, sslParameters) -> "main";
    }

}
