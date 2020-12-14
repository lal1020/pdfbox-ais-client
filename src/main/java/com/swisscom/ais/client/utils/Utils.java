package com.swisscom.ais.client.utils;

import java.util.Properties;
import java.util.StringTokenizer;
import java.util.UUID;

public class Utils {

    public static String generateRequestId() {
        return "ID-" + UUID.randomUUID().toString();
    }

    public static String generateDocumentId() {
        return "DOC-" + System.currentTimeMillis();
    }

    public static String getNotNull(Properties properties, String propertyName) {
        String value = properties.getProperty(propertyName);
        if (value == null) {
            throw new IllegalStateException("Invalid configuration. The [" + propertyName + "] is missing or is empty");
        }
        return value;
    }

    public static String[] getStringArray(String csv) {
        StringTokenizer tokenizer = new StringTokenizer(csv, ",");
        String[] result = new String[tokenizer.countTokens()];
        for (int index = 0; index < result.length; index++) {
            result[index] = tokenizer.nextToken().trim();
        }
        return result;
    }

}
