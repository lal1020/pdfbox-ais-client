package com.swisscom.ais.client.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Loggers {

    public static final String CLIENT = "swisscom.ais.client";
    public static final String CONFIG = "swisscom.ais.client.config";
    public static final String CLIENT_PROTOCOL = "swisscom.ais.client.protocol";
    public static final String REQUEST_RESPONSE = "swisscom.ais.client.requestResponse";
    public static final String FULL_REQUEST_RESPONSE = "swisscom.ais.client.fullRequestResponse";
    public static final String PDF_PROCESSING = "swisscom.ais.client.pdfProcessing";

    // ----------------------------------------------------------------------------------------------------

    public static final List<String> ALL_OF_THEM;

    static {
        Field[] allFields = Loggers.class.getDeclaredFields();
        ALL_OF_THEM = new LinkedList<>();
        for (Field field : allFields) {
            if (Modifier.isStatic(field.getModifiers()) && !field.getName().equals("ALL_OF_THEM")) {
                try {
                    ALL_OF_THEM.add((String) field.get(Loggers.class));
                } catch (IllegalAccessException ignored) {
                    // ignored exception
                }
            }
        }
        Collections.sort(ALL_OF_THEM);
    }

}
