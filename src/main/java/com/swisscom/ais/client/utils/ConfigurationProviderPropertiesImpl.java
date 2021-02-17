package com.swisscom.ais.client.utils;

import java.util.Properties;

/**
 * Implementation of {@link ConfigurationProvider} that takes the requested properties from a standard Java {@link Properties} store.
 */
public class ConfigurationProviderPropertiesImpl implements ConfigurationProvider {

    private final Properties properties;

    public ConfigurationProviderPropertiesImpl(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String getProperty(String name) {
        return properties.getProperty(name);
    }

}
