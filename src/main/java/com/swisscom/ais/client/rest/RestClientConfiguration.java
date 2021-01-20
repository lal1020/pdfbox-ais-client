package com.swisscom.ais.client.rest;

import com.swisscom.ais.client.AisClientException;

import java.io.IOException;
import java.util.Properties;

import static com.swisscom.ais.client.utils.Utils.getIntNotNull;
import static com.swisscom.ais.client.utils.Utils.getStringNotNull;

public class RestClientConfiguration {

    private static final int CLIENT_MAX_CONNECTION_TOTAL = 20;
    private static final int CLIENT_MAX_CONNECTIONS_PER_ROUTE = 10;
    private static final int CLIENT_SOCKET_TIMEOUT_IN_SEC = 10;
    private static final int CLIENT_RESPONSE_TIMEOUT_IN_SEC = 20;

    // ----------------------------------------------------------------------------------------------------

    private String restServiceSignUrl = "https://ais.swisscom.com/AIS-Server/rs/v1.0/sign";
    private String restServicePendingUrl = "https://ais.swisscom.com/AIS-Server/rs/v1.0/pending";

    private String clientKeyFile;
    private String clientKeyPassword;

    private String clientCertificateFile;
    private String serverCertificateFile;

    private int maxTotalConnections = CLIENT_MAX_CONNECTION_TOTAL;
    private int maxConnectionsPerRoute = CLIENT_MAX_CONNECTIONS_PER_ROUTE;
    private int connectionTimeoutInSec = CLIENT_SOCKET_TIMEOUT_IN_SEC;
    private int responseTimeoutInSec = CLIENT_RESPONSE_TIMEOUT_IN_SEC;

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

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    public int getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
    }

    public int getConnectionTimeoutInSec() {
        return connectionTimeoutInSec;
    }

    public void setConnectionTimeoutInSec(int connectionTimeoutInSec) {
        this.connectionTimeoutInSec = connectionTimeoutInSec;
    }

    public int getResponseTimeoutInSec() {
        return responseTimeoutInSec;
    }

    public void setResponseTimeoutInSec(int responseTimeoutInSec) {
        this.responseTimeoutInSec = responseTimeoutInSec;
    }

    // ----------------------------------------------------------------------------------------------------

    public void setFromPropertiesClasspathFile(String fileName) {
        Properties properties;
        try {
            properties = new Properties();
            properties.load(this.getClass().getResourceAsStream(fileName));
        } catch (IOException exception) {
            throw new AisClientException("Failed to load REST client properties from classpath file: [" + fileName + "]", exception);
        }
        setFromProperties(properties);
    }

    public void setFromProperties(Properties properties) {
        restServiceSignUrl = getStringNotNull(properties, "server.rest.signUrl");
        restServicePendingUrl = getStringNotNull(properties, "server.rest.pendingUrl");
        clientKeyFile = getStringNotNull(properties, "client.auth.keyFile");
        clientKeyPassword = getStringNotNull(properties, "client.auth.keyPassword");
        clientCertificateFile = getStringNotNull(properties, "client.cert.file");
        serverCertificateFile = getStringNotNull(properties, "server.cert.file");
        maxTotalConnections = getIntNotNull(properties, "client.http.maxTotalConnections");
        maxConnectionsPerRoute = getIntNotNull(properties, "client.http.maxConnectionsPerRoute");
        connectionTimeoutInSec = getIntNotNull(properties, "client.http.connectionTimeoutInSeconds");
        responseTimeoutInSec = getIntNotNull(properties, "client.http.responseTimeoutInSeconds");
    }

}
