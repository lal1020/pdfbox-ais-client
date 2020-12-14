package com.swisscom.ais.client.utils;

import java.util.UUID;

public class Utils {

    public static String generateRequestId() {
        return "ID-" + UUID.randomUUID().toString();
    }

    public static String generateDocumentId() {
        return "DOC-" + System.currentTimeMillis();
    }

}
