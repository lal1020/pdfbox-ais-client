package com.swisscom.ais.client.utils;

import com.swisscom.ais.client.AisClientException;

import java.util.Properties;
import java.util.UUID;

public class Utils {

    public static String generateRequestId() {
        return "ID-" + UUID.randomUUID().toString();
    }

    public static String generateDocumentId() {
        return "DOC-" + System.currentTimeMillis();
    }

    public static String getStringNotNull(Properties properties, String propertyName) {
        String value = properties.getProperty(propertyName);
        if (value == null) {
            throw new IllegalStateException("Invalid configuration. The [" + propertyName + "] is missing or is empty");
        }
        return value;
    }

    public static int getIntNotNull(Properties properties, String propertyName) {
        String value = properties.getProperty(propertyName);
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

    public static void valueBetween(int value, int minValue, int maxValue, String errorMessage, Trace trace) throws AisClientException {
        if (value < minValue || value > maxValue) {
            if (trace == null) {
                throw new AisClientException(errorMessage);
            } else {
                throw new AisClientException(errorMessage + " - " + trace.getId());
            }
        }
    }

}
