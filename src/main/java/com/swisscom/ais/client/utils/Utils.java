package com.swisscom.ais.client.utils;

import com.swisscom.ais.client.AisClientException;
import com.swisscom.ais.client.Cli;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Utils {

    public static String generateRequestId() {
        return "ID-" + UUID.randomUUID().toString();
    }

    public static String generateDocumentId() {
        return "DOC-" + System.currentTimeMillis();
    }

    public static String getStringNotNull(ConfigurationProvider provider, String propertyName) {
        String value = provider.getProperty(propertyName);
        if (value == null) {
            throw new IllegalStateException("Invalid configuration. The [" + propertyName + "] is missing or is empty");
        }
        return value;
    }

    public static int getIntNotNull(ConfigurationProvider provider, String propertyName) {
        String value = provider.getProperty(propertyName);
        if (value == null) {
            throw new IllegalStateException("Invalid configuration. The [" + propertyName + "] is missing or is empty");
        }
        return Integer.parseInt(value);
    }

    public static String stripInnerLargeBase64Content(String source, char leftBoundChar, char rightBoundChar) {
        String pattern = leftBoundChar + "[A-Za-z0-9+\\\\/=_-]{500,}" + rightBoundChar;
        String replacement = leftBoundChar + "..." + rightBoundChar;
        return source.replaceAll(pattern, replacement);
    }

    public static void valueNotEmpty(String value, String errorMessage, Trace trace) throws AisClientException {
        if (value == null || value.trim().length() == 0) {
            if (trace == null) {
                throw new AisClientException(errorMessage);
            } else {
                throw new AisClientException(errorMessage + " - " + trace.getId());
            }
        }
    }

    public static void valueNotNull(Object value, String errorMessage, Trace trace) throws AisClientException {
        if (value == null) {
            if (trace == null) {
                throw new AisClientException(errorMessage);
            } else {
                throw new AisClientException(errorMessage + " - " + trace.getId());
            }
        }
    }

    public static void valueBetween(int value, int minValue, int maxValue, String errorMessage, Trace trace) throws AisClientException {
        if (value < minValue || value > maxValue) {
            if (trace == null) {
                throw new AisClientException(errorMessage);
            } else {
                throw new AisClientException(errorMessage + " - " + trace.getId());
            }
        }
    }

    public static void copyFileFromClasspathToDisk(String inputFile, String outputFile) {
        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            InputStream is = Cli.class.getResourceAsStream(inputFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) > 0) {
                fos.write(buffer, 0, bytesRead);
            }
            is.close();
            fos.close();
        } catch (IOException e) {
            throw new AisClientException("Failed to create the file: [" + outputFile + "]");
        }
    }

    public static void copyFileFromClasspathToStdout(String inputFile) {
        try {
            InputStream is = Cli.class.getResourceAsStream(inputFile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) > 0) {
                baos.write(buffer, 0, bytesRead);
            }
            is.close();
            baos.close();
            System.out.println(new String(baos.toByteArray(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AisClientException("Failed to copy file: [" + inputFile + "] to STDOUT");
        }
    }

}
