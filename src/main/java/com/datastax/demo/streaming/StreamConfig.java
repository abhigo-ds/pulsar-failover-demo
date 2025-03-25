package com.datastax.demo.streaming;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class StreamConfig {

	private static final int MAX_CLUSTERS = 10;

	private List<Cluster> clusters;
	private Cluster defaultCluster;
	private String topicFullPath;
	private String subscription;
	private String providerUrl;
	private int providerPort;

	public StreamConfig() {
		this.clusters = loadClusters();
		this.topicFullPath = PropertiesLoader.getProperty("topic.fullpath");
		this.subscription = PropertiesLoader.getProperty("subscription");
		this.providerUrl = PropertiesLoader.getProperty("provider.url");
		this.providerPort = Integer.parseInt(PropertiesLoader.getProperty("provider.port"));
	}

	private List<Cluster> loadClusters() {
		List<Cluster> clusters = new ArrayList<>();
		IntStream.range(0, MAX_CLUSTERS).forEach(i -> {
			String clusterPrefix = "clusters." + i;
			Cluster cluster = new Cluster();
			String name = PropertiesLoader.getProperty(clusterPrefix + ".name");
			if (name == null) {
				return;
			}
			cluster.setName(name);
			cluster.setUrl(PropertiesLoader.getProperty(clusterPrefix + ".url"));
			cluster.setToken(PropertiesLoader.getProperty(clusterPrefix + ".token"));
			cluster.setRegion(PropertiesLoader.getProperty(clusterPrefix + ".region"));
			if ("true".equals(PropertiesLoader.getProperty(clusterPrefix + ".default"))) {
				cluster.setDefault(true);
				defaultCluster = cluster;
			}
			clusters.add(cluster);
		});

		if (defaultCluster == null) {
			defaultCluster = clusters.get(0);
		}

		return clusters;
	}

	public List<Cluster> getClusters() {
		return clusters;
	}

	public Cluster getDefaultCluster() {
		return defaultCluster;
	}

	public String getServiceUrlDefault() {
		return defaultCluster.getUrl();
	}

	public String getServiceUrl(String clusterName) {
		return PropertiesLoader.getProperty("service.url." + clusterName);
	}

	public String getAuthTokenDefault() {
		return defaultCluster.getToken();
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
