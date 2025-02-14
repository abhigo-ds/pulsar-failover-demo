package com.datastax.demo.streaming;

import java.util.List;

public class StreamConfig {

	private List<String> clusters;
	private String clusterDefault;
	private String topicFullPath;
	private String subscription;
	private String providerUrl;
	private int providerPort;

	public StreamConfig() {
		this.clusters = List.of(PropertiesLoader.getProperty("clusters").split(","));
		this.clusterDefault = PropertiesLoader.getProperty("cluster.default");
		this.topicFullPath = PropertiesLoader.getProperty("topic.fullpath");
		this.subscription = PropertiesLoader.getProperty("subscription");
		this.providerUrl = PropertiesLoader.getProperty("provider.url");
		this.providerPort = Integer.parseInt(PropertiesLoader.getProperty("provider.port"));
	}

	public List<String> getClusters() {
		return clusters;
	}

	public String getClusterDefault() {
		return clusterDefault;
	}

	public String getServiceUrlDefault() {
		return getServiceUrl(clusterDefault);
	}

	public String getServiceUrl(String clusterName) {
		return PropertiesLoader.getProperty("service.url." + clusterName);
	}

	public String getAuthTokenDefault() {
		return getAuthToken(clusterDefault);
	}

	public String getAuthToken(String name) {
		return PropertiesLoader.getProperty("auth.token." + name);
	}

	public String getTopicFullPath() {
		return topicFullPath;
	}

	public String getSubscription() {
		return subscription;
	}

	public int getProducerDelayMillis() {
		return Integer.parseInt(PropertiesLoader.getProperty("producer.delay.millis"));
	}

	public int getConsumerDelayMillis() {
		return Integer.parseInt(PropertiesLoader.getProperty("consumer.delay.millis"));
	}

	public int getConsumerWaitSeconds() {
		return Integer.parseInt(PropertiesLoader.getProperty("consumer.wait.seconds"));
	}

	public String getProviderUrl() {
		return providerUrl;
	}

	public int getProviderPort() {
		return providerPort;
	}

}
