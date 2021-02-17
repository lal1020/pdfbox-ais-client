package com.swisscom.ais.client.impl;

import com.swisscom.ais.client.utils.Loggers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class ClientVersionProvider {

    private static final Logger log = LoggerFactory.getLogger(Loggers.CLIENT);
    private boolean versionInfoAvailable = false;
    private String versionInfo;

    public void init() {
        InputStream inputStream = this.getClass().getResourceAsStream("/build.properties");
        if (inputStream == null) {
            log.debug("No build.properties file was found in the pdfbox AIS client JAR. Skipping version info logging");
            return;
        }

        try {
            Properties properties = new Properties();
            properties.load(inputStream);

            String version = properties.getProperty("build.version");
            String timestamp = properties.getProperty("build.timestamp");
            String gitId = properties.getProperty("build.git.id");

            StringBuilder builder = new StringBuilder();
            if (version != null) {
                builder.append("version ").append(version);
                builder.append(", built on ").append(timestamp);
            }
            if (gitId != null) {
                builder.append(", git #").append(gitId);
            }

            versionInfo = builder.toString();
            versionInfoAvailable = true;
            inputStream.close();
        } catch (Exception ignored) {
            log.debug("Failed to load the AIS client version info from embedded build.properties file. Skipping version info logging");
        }
    }

    // ----------------------------------------------------------------------------------------------------

    public boolean isVersionInfoAvailable() {
        return versionInfoAvailable;
    }

    public String getVersionInfo() {
        return versionInfo;
    }
}
