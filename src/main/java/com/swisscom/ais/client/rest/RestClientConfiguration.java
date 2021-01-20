package com.swisscom.ais.client.rest;

import com.swisscom.ais.client.AisClientException;

import java.io.IOException;
import java.util.Properties;

import static com.swisscom.ais.client.utils.Utils.getIntNotNull;
import static com.swisscom.ais.client.utils.Utils.getStringNotNull;
import static com.swisscom.ais.client.utils.Utils.valueBetween;
import static com.swisscom.ais.client.utils.Utils.valueNotEmpty;

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
        valueNotEmpty(clientKeyFile,
                      "The clientKeyFile parameter of the REST client configuration must not be empty", null);
        this.clientKeyFile = clientKeyFile;
    }

    public String getClientKeyPassword() {
        return clientKeyPassword;
    }

    public void setClientKeyPassword(String clientKeyPassword) {
        valueNotEmpty(clientKeyPassword,
                      "The clientKeyPassword parameter of the REST client configuration must not be empty", null);
        this.clientKeyPassword = clientKeyPassword;
    }

    public String getClientCertificateFile() {
        return clientCertificateFile;
    }

    public void setClientCertificateFile(String clientCertificateFile) {
        valueNotEmpty(clientCertificateFile,
                      "The clientCertificateFile parameter of the REST client configuration must not be empty", null);
        this.clientCertificateFile = clientCertificateFile;
    }

    public String getServerCertificateFile() {
        return serverCertificateFile;
    }

    public void setServerCertificateFile(String serverCertificateFile) {
        valueNotEmpty(serverCertificateFile,
                      "The serverCertificateFile parameter of the REST client configuration must not be empty", null);
        this.serverCertificateFile = serverCertificateFile;
    }

    public String getRestServiceSignUrl() {
        return restServiceSignUrl;
    }

    public void setRestServiceSignUrl(String restServiceSignUrl) {
        valueNotEmpty(restServiceSignUrl,
                      "The restServiceSignUrl parameter of the REST client configuration must not be empty", null);
        this.restServiceSignUrl = restServiceSignUrl;
    }

    public String getRestServicePendingUrl() {
        return restServicePendingUrl;
    }

    public void setRestServicePendingUrl(String restServicePendingUrl) {
        valueNotEmpty(restServicePendingUrl,
                      "The restServicePendingUrl parameter of the REST client configuration must not be empty", null);
        this.restServicePendingUrl = restServicePendingUrl;
    }

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        valueBetween(maxTotalConnections, 2, 100,
                     "The maxTotalConnections parameter of the REST client configuration must be between 2 and 100", null);
        this.maxTotalConnections = maxTotalConnections;
    }

    public int getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
        valueBetween(maxConnectionsPerRoute, 2, 100,
                     "The maxConnectionsPerRoute parameter of the REST client configuration must be between 2 and 100", null);
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
    }

    public int getConnectionTimeoutInSec() {
        return connectionTimeoutInSec;
    }

    public void setConnectionTimeoutInSec(int connectionTimeoutInSec) {
        valueBetween(connectionTimeoutInSec, 2, 100,
                     "The connectionTimeoutInSec parameter of the REST client configuration must be between 2 and 100", null);
        this.connectionTimeoutInSec = connectionTimeoutInSec;
    }

    public int getResponseTimeoutInSec() {
        return responseTimeoutInSec;
    }

    public void setResponseTimeoutInSec(int responseTimeoutInSec) {
        valueBetween(responseTimeoutInSec, 2, 100,
                     "The responseTimeoutInSec parameter of the REST client configuration must be between 2 and 100", null);
        this.responseTimeoutInSec = responseTimeoutInSec;
    }

    // ----------------------------------------------------------------------------------------------------

    @SuppressWarnings("unused")
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
        setRestServiceSignUrl(getStringNotNull(properties, "server.rest.signUrl"));
        setRestServicePendingUrl(getStringNotNull(properties, "server.rest.pendingUrl"));
        setClientKeyFile(getStringNotNull(properties, "client.auth.keyFile"));
        setClientKeyPassword(getStringNotNull(properties, "client.auth.keyPassword"));
        setClientCertificateFile(getStringNotNull(properties, "client.cert.file"));
        setServerCertificateFile(getStringNotNull(properties, "server.cert.file"));
        setMaxTotalConnections(getIntNotNull(properties, "client.http.maxTotalConnections"));
        setMaxConnectionsPerRoute(getIntNotNull(properties, "client.http.maxConnectionsPerRoute"));
        setConnectionTimeoutInSec(getIntNotNull(properties, "client.http.connectionTimeoutInSeconds"));
        setResponseTimeoutInSec(getIntNotNull(properties, "client.http.responseTimeoutInSeconds"));
    }

}
