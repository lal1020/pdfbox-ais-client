package com.swisscom.ais.client.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static com.swisscom.ais.client.utils.Utils.getNotNull;

public class RestClientConfiguration {

    private String restServiceSignUrl = "https://ais.swisscom.com/AIS-Server/rs/v1.0/sign";
    private String restServicePendingUrl = "https://ais.swisscom.com/AIS-Server/rs/v1.0/pending";

    private String clientKeyFile;
    private String clientKeyPassword;

    private String clientCertificateFile;
    private String serverCertificateFile;

    // ----------------------------------------------------------------------------------------------------

    public String getClientKeyFile() {
        return clientKeyFile;
    }

    public void setClientKeyFile(String clientKeyFile) {
        this.clientKeyFile = clientKeyFile;
    }

    public String getClientKeyPassword() {
        return clientKeyPassword;
    }

    public void setClientKeyPassword(String clientKeyPassword) {
        this.clientKeyPassword = clientKeyPassword;
    }

    public String getClientCertificateFile() {
        return clientCertificateFile;
    }

    public void setClientCertificateFile(String clientCertificateFile) {
        this.clientCertificateFile = clientCertificateFile;
    }

    public String getServerCertificateFile() {
        return serverCertificateFile;
    }

    public void setServerCertificateFile(String serverCertificateFile) {
        this.serverCertificateFile = serverCertificateFile;
    }

    public String getRestServiceSignUrl() {
        return restServiceSignUrl;
    }

    public void setRestServiceSignUrl(String restServiceSignUrl) {
        this.restServiceSignUrl = restServiceSignUrl;
    }

    public String getRestServicePendingUrl() {
        return restServicePendingUrl;
    }

    public void setRestServicePendingUrl(String restServicePendingUrl) {
        this.restServicePendingUrl = restServicePendingUrl;
    }

    // ----------------------------------------------------------------------------------------------------

    public void setFromPropertiesClasspathFile(String fileName) {
        try {
            loadFromPropertiesInputStream(this.getClass().getResourceAsStream(fileName));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setFromProperties(Properties properties) {
        loadFromProperties(properties);
    }

    // ----------------------------------------------------------------------------------------------------

    private void loadFromPropertiesInputStream(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        loadFromProperties(properties);
    }

    private void loadFromProperties(Properties properties) {
        restServiceSignUrl = getNotNull(properties, "server.rest.signUrl");
        restServicePendingUrl = getNotNull(properties, "server.rest.pendingUrl");
        clientKeyFile = getNotNull(properties, "client.auth.keyFile");
        clientKeyPassword = getNotNull(properties, "client.auth.keyPassword");
        clientCertificateFile = getNotNull(properties, "client.cert.file");
        serverCertificateFile = getNotNull(properties, "server.cert.file");
    }

}
