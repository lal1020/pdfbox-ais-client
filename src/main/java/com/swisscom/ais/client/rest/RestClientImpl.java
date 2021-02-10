package com.swisscom.ais.client.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swisscom.ais.client.AisClientException;
import com.swisscom.ais.client.rest.model.pendingreq.AISPendingRequest;
import com.swisscom.ais.client.rest.model.signreq.AISSignRequest;
import com.swisscom.ais.client.rest.model.signresp.AISSignResponse;
import com.swisscom.ais.client.utils.Loggers;
import com.swisscom.ais.client.utils.Trace;
import com.swisscom.ais.client.utils.Utils;

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

    private static final Logger logClient = LoggerFactory.getLogger(Loggers.CLIENT);
    private static final Logger logProtocol = LoggerFactory.getLogger(Loggers.CLIENT_PROTOCOL);
    private static final Logger logReqResp = LoggerFactory.getLogger(Loggers.REQUEST_RESPONSE);
    private static final Logger logFullReqResp = LoggerFactory.getLogger(Loggers.FULL_REQUEST_RESPONSE);

    private RestClientConfiguration config;
    private ObjectMapper jacksonMapper;
    private CloseableHttpClient httpClient;

    // ----------------------------------------------------------------------------------------------------

    public void setConfiguration(RestClientConfiguration config) {
        this.config = config;
        Security.addProvider(new BouncyCastleProvider());
        jacksonMapper = new ObjectMapper();
        jacksonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        jacksonMapper.setSerializationInclusion(JsonInclude.Include.USE_DEFAULTS);

        SSLConnectionSocketFactory sslConnectionSocketFactory;
        try {
            SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                .loadKeyMaterial(produceTheKeyStore(config),
                                 keyToCharArray(config.getClientKeyPassword()), produceAPrivateKeyStrategy());
            sslContextBuilder.loadTrustMaterial(produceTheTrustStore(config), null);
            sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build());
        } catch (Exception e) {
            throw new AisClientException("Failed to configure the TLS/SSL connection factory for the AIS client", e);
        }

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(config.getMaxTotalConnections())
            .setMaxConnPerRoute(config.getMaxConnectionsPerRoute())
            .setSSLSocketFactory(sslConnectionSocketFactory)
            .build();
        RequestConfig httpClientRequestConfig = RequestConfig.custom()
            .setConnectTimeout(config.getConnectionTimeoutInSec(), TimeUnit.SECONDS)
            .setResponseTimeout(config.getResponseTimeoutInSec(), TimeUnit.SECONDS)
            .build();

        httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(httpClientRequestConfig)
            .build();
    }

    @Override
    public void close() throws IOException {
        logClient.info("Closing the REST client");
        if (httpClient != null) {
            logClient.info("Closing the embedded HTTP client");
            httpClient.close();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    @Override
    public AISSignResponse requestSignature(AISSignRequest requestWrapper, Trace trace) {
        return sendAndReceive("SignRequest", config.getRestServiceSignUrl(),
                              requestWrapper, AISSignResponse.class, trace);
    }

    @Override
    public AISSignResponse pollForSignatureStatus(AISPendingRequest requestWrapper, Trace trace) {
        return sendAndReceive("PendingRequest", config.getRestServicePendingUrl(),
                              requestWrapper, AISSignResponse.class, trace);
    }

    // ----------------------------------------------------------------------------------------------------

    private <TReq, TResp> TResp sendAndReceive(String operationName,
                                               String serviceUrl,
                                               TReq requestObject,
                                               @SuppressWarnings("SameParameterValue") Class<TResp> responseClass,
                                               Trace trace) {
        logProtocol.debug("{}: Serializing object of type {} to JSON - {}",
                          operationName, requestObject.getClass().getSimpleName(), trace.getId());
        String requestJson;
        try {
            requestJson = jacksonMapper.writeValueAsString(requestObject);
        } catch (JsonProcessingException e) {
            throw new AisClientException("Failed to serialize request object to JSON, for operation " +
                                         operationName + " - " + trace.getId(), e);
        }

        HttpPost httpPost = new HttpPost(serviceUrl);
        httpPost.setEntity(new StringEntity(requestJson, ContentType.APPLICATION_JSON, CharEncoding.UTF_8, false));
        httpPost.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON);
        logProtocol.info("{}: Sending request to: [{}] - {}", operationName, serviceUrl, trace.getId());
        logReqResp.info("{}: Sending JSON to: [{}], content: [{}] - {}", operationName, serviceUrl, requestJson, trace.getId());
        logFullReqResp.info("{}: Sending JSON to: [{}], content: [{}] - {}", operationName, serviceUrl, requestJson, trace.getId());

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            logProtocol.info("{}: Received HTTP status code: {} - {}", operationName, response.getCode(), trace.getId());
            String responseJson;
            try {
                responseJson = EntityUtils.toString(response.getEntity());
            } catch (ParseException e) {
                throw new AisClientException("Failed to interpret the HTTP response content as a string, for operation " +
                                             operationName + " - " + trace.getId(), e);
            }
            if (response.getCode() == 200) {
                if (logReqResp.isInfoEnabled()) {
                    String strippedResponse = Utils.stripInnerLargeBase64Content(responseJson, '"', '"');
                    logReqResp.info("{}: Received JSON content: {} - {}", operationName, strippedResponse, trace.getId());
                }
                if (logFullReqResp.isInfoEnabled()) {
                    logFullReqResp.info("{}: Received JSON content: {} - {}", operationName, responseJson, trace.getId());
                }
                logProtocol.debug("{}: Deserializing JSON to object of type {} - {}", operationName, responseClass.getSimpleName(), trace.getId());
                try {
                    return jacksonMapper.readValue(responseJson, responseClass);
                } catch (JsonProcessingException e) {
                    throw new AisClientException("Failed to deserialize JSON content to object of type " +
                                                 responseClass.getSimpleName() + " for operation " +
                                                 operationName + " - " +
                                                 trace.getId(), e);
                }
            } else {
                throw new AisClientException("Received fault response: HTTP " +
                                             response.getCode() + " " +
                                             response.getReasonPhrase() + " - " + trace.getId());
            }
        } catch (SSLException e) {
            throw new AisClientException("TLS/SSL connection failure for " + operationName + " - " + trace.getId(), e);
        } catch (Exception e) {
            throw new AisClientException("Communication failure for " + operationName + " - " + trace.getId(), e);
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
            keyStore.setKeyEntry("main", privateKey, keyToCharArray(config.getClientKeyPassword()), new Certificate[]{certificate});

            return keyStore;
        } catch (Exception e) {
            throw new AisClientException("Failed to initialize the TLS keystore", e);
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
            throw new AisClientException("Failed to initialize the TLS truststore", e);
        }
    }

    public static PrivateKey getPrivateKey(String filename, String keyPassword) throws IOException {
        PEMParser pemParser = new PEMParser(new InputStreamReader(new FileInputStream(filename)));
        PEMKeyPair keyPair;
        if (Utils.isEmpty(keyPassword)) {
            keyPair = (PEMKeyPair) pemParser.readObject();
        } else {
            PEMEncryptedKeyPair encryptedKeyPair = (PEMEncryptedKeyPair) pemParser.readObject();
            PEMDecryptorProvider decryptorProvider = new JcePEMDecryptorProviderBuilder().setProvider("BC").build(keyPassword.toCharArray());
            keyPair = encryptedKeyPair.decryptKeyPair(decryptorProvider);
        }
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
    }

    private PrivateKeyStrategy produceAPrivateKeyStrategy() {
        return (aliases, sslParameters) -> "main";
    }

    private char[] keyToCharArray(String key) {
        return Utils.isEmpty(key) ? new char[0] : key.toCharArray();
    }

}
