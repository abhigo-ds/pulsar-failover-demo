package com.datastax.demo.streaming;

public class Cluster {

	private static final String authPluginClassName = "org.apache.pulsar.client.impl.auth.AuthenticationToken";

	private String name;
	private String url;
	private String region;
	private String token;
	private boolean isDefault;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public String toJson() {
		return String.format(
				"{" + "\"serviceUrl\":\"%s\", " + "\"authPluginClassName\":\"%s\","
						+ "\"tlsHostnameVerificationEnable\":\"true\"," + "\"authParamsString\":\"%s\"}",
				url, authPluginClassName, token);
	}
}
