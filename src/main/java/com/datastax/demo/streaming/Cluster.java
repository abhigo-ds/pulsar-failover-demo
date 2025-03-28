package com.datastax.demo.streaming;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Cluster {

	private static final String AUTH_CLASS_NAME = "org.apache.pulsar.client.impl.auth.AuthenticationToken";
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private String name;
	private String serviceUrl;
	private String authPluginClassName = AUTH_CLASS_NAME;
	private boolean tlsHostnameVerificationEnable = true;
	private String authParamsString;
	private String region;
	private boolean isDefault;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getServiceUrl() {
		return serviceUrl;
	}

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public String getAuthPluginClassName() {
		return authPluginClassName;
	}

	public boolean isTlsHostnameVerificationEnable() {
		return tlsHostnameVerificationEnable;
	}

	public String getAuthParamsString() {
		return authParamsString;
	}

	public void setAuthParamsString(String authParamsString) {
		this.authParamsString = authParamsString;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public String toJson() throws JsonProcessingException {
		return MAPPER.writeValueAsString(this);
	}

}
