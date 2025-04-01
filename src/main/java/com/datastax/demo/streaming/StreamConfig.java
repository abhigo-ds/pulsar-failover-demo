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
	private int expireSeconds;

	public StreamConfig() {
		this.clusters = loadClusters();
		this.topicFullPath = PropertiesLoader.getProperty("topic.fullpath");
		this.subscription = PropertiesLoader.getProperty("subscription");
		this.providerUrl = PropertiesLoader.getProperty("provider.url");
		this.providerPort = Integer.parseInt(PropertiesLoader.getProperty("provider.port"));
		this.expireSeconds = Integer.parseInt(PropertiesLoader.getProperty("provider.expire.seconds"));
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
			cluster.setServiceUrl(PropertiesLoader.getProperty(clusterPrefix + ".url"));
			cluster.setAuthParamsString(PropertiesLoader.getProperty(clusterPrefix + ".token"));
			cluster.setRegion(PropertiesLoader.getProperty(clusterPrefix + ".region"));
			if ("true".equals(PropertiesLoader.getProperty(clusterPrefix + ".default"))) {
				cluster.setDefault(true);
				defaultCluster = cluster;
			}
			clusters.add(cluster);
		});

		if (defaultCluster == null && clusters.size() > 0) {
			clusters.get(0).setDefault(true);
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

	public int getExpireSeconds() {
		return expireSeconds;
	}

}
