package com.datastax.demo.streaming;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {

	private static final String PROPERTIES_FILE_NAME = "application.properties";
	private static Properties properties = new Properties();

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

	public static String getProperty(String key) {
		return properties.getProperty(key);
	}

	public static Properties getProperties() {
		return properties;
	}

}
