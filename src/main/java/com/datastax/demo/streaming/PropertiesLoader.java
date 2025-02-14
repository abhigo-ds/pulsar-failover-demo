package com.datastax.demo.streaming;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {

    private static final String PROPERTIES_FILE_NAME = "application.properties";
    private static Properties properties = new Properties();

    // Static block to load properties when the class is first accessed.
    static {
        try (InputStream inputStream = PropertiesLoader.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE_NAME)) {
            if (inputStream != null) {
                properties.load(inputStream);
            } else {
                throw new RuntimeException("Property file '" + PROPERTIES_FILE_NAME + "' not found in the classpath");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading properties file", e);
        }
    }

    /**
     * Retrieves the property value for the given key.
     *
     * @param key the property key to look up
     * @return the property value associated with the key, or null if not found
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Returns all the loaded properties.
     *
     * @return the properties object containing all loaded properties
     */
    public static Properties getProperties() {
        return properties;
    }

}
