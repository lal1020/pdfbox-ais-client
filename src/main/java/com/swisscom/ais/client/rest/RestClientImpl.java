package com.swisscom.ais.client.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swisscom.ais.client.AisClient;
import com.swisscom.ais.client.rest.model.pendingreq.AISPendingRequest;
import com.swisscom.ais.client.rest.model.signreq.AISSignRequest;
import com.swisscom.ais.client.rest.model.signresp.AISSignResponse;

import org.apache.commons.codec.CharEncoding;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.PrivateKeyStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

public class RestClientImpl implements RestClient {

    private static final int CLIENT_MAX_CONNECTION_TOTAL = 20;
    private static final int CLIENT_MAX_CONNECTIONS_PER_ROUTE = 10;
    private static final int CLIENT_SOCKET_TIMEOUT_IN_MS = 10000; // 10s
    private static final int CLIENT_RESPONSE_TIMEOUT_IN_MS = 20000; // 20s

    // ----------------------------------------------------------------------------------------------------

    private static final Logger log = LoggerFactory.getLogger(AisClient.class);

    private RestClientConfiguration config;
    private ObjectMapper jacksonMapper;
    private CloseableHttpClient httpClient;

    // ----------------------------------------------------------------------------------------------------

    public void setConfiguration(RestClientConfiguration config) {
        this.config = config;
        Security.addProvider(new BouncyCastleProvider());
        jacksonMapper = new ObjectMapper();

        SSLConnectionSocketFactory sslConnectionSocketFactory;
        try {
            SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                .loadKeyMaterial(produceTheKeyStore(config),
                                 config.getClientKeyPassword().toCharArray(), produceAPrivateKeyStrategy());
            sslContextBuilder.loadTrustMaterial(produceTheTrustStore(config), null);
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

    @Override
    public AISSignResponse requestSignature(AISSignRequest requestWrapper) {
        return sendAndReceive("SignRequest", config.getRestServiceSignUrl(),
                              requestWrapper, AISSignResponse.class);
    }

    @Override
    public AISSignResponse pollForSignatureStatus(AISPendingRequest requestWrapper) {
        return sendAndReceive("PendingRequest", config.getRestServicePendingUrl(),
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
        httpPost.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON);
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

    private KeyStore produceTheKeyStore(RestClientConfiguration config) {
        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            FileInputStream is = new FileInputStream(config.getClientCertificateFile());
            X509Certificate certificate = (X509Certificate) fact.generateCertificate(is);
            PrivateKey privateKey = getPrivateKey(config.getClientKeyFile(), config.getClientKeyPassword());

            KeyStore keyStore = KeyStore.getInstance("jks");
            keyStore.load(null, null);
            // the keystore password is not used at this point, can be any value
            keyStore.setKeyEntry("main", privateKey, "secret".toCharArray(), new Certificate[]{certificate});

            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize the TLS keystore", e);
        }
    }

    private KeyStore produceTheTrustStore(RestClientConfiguration config) {
        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            FileInputStream is = new FileInputStream(config.getServerCertificateFile());
            X509Certificate certificate = (X509Certificate) fact.generateCertificate(is);

            KeyStore keyStore = KeyStore.getInstance("jks");
            keyStore.load(null, null);
            keyStore.setCertificateEntry("main", certificate);

            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize the TLS truststore", e);
        }
    }

    public static PrivateKey getPrivateKey(String filename, String keyPassword) throws IOException {
        PEMParser pemParser = new PEMParser(new InputStreamReader(new FileInputStream(filename)));
        PEMEncryptedKeyPair encryptedKeyPair = (PEMEncryptedKeyPair) pemParser.readObject();
        PEMDecryptorProvider decryptorProvider = new JcePEMDecryptorProviderBuilder().build(keyPassword.toCharArray());
        PEMKeyPair pemKeyPair = encryptedKeyPair.decryptKeyPair(decryptorProvider);

        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        return converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());
    }

    private PrivateKeyStrategy produceAPrivateKeyStrategy() {
        return (aliases, sslParameters) -> "main";
    }

}
