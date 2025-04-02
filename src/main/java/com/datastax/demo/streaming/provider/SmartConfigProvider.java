package com.datastax.demo.streaming.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.map.PassiveExpiringMap;

import com.datastax.demo.streaming.Cluster;
import com.datastax.demo.streaming.StreamConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class SmartConfigProvider {

	private static Map<String, Instance> instances = null;
	private static final Map<String, Group> groups = new ConcurrentHashMap<>();
	private static final Map<String, Cluster> clusters = new ConcurrentHashMap<>();
	private static final Map<String, Group> regions = new ConcurrentHashMap<>();

	private static final ObjectMapper mapper = new ObjectMapper();

	public static void main(String[] args) throws IOException {
		StreamConfig config = new StreamConfig();
		instances = new PassiveExpiringMap<>(TimeUnit.SECONDS.toMillis(config.getExpireSeconds()));
		mapClusterToRegionAndDefaultGroup(config);

		HttpServer server = HttpServer.create(new InetSocketAddress(config.getProviderPort()), 0);
		server.createContext("/getconfig", new GetConfigHandler());
		server.createContext("/instances", new InstanceHandler());
		server.createContext("/groups", new GroupHandler());
		server.createContext("/clusters", new ClusterHandler());

		server.setExecutor(null);
		server.start();
		System.out.println("SmartConfigProvider started at port " + config.getProviderPort());
	}

	private static void mapClusterToRegionAndDefaultGroup(StreamConfig config) {
		config.getClusters().forEach(cluster -> {
			clusters.put(cluster.getName(), cluster);
			Group group = new Group("Default-" + cluster.getName() + "-Group", cluster.getName());
			groups.put(group.name, group);
			System.out.println("Created default group " + group.name + " for cluster " + cluster.getName());
			regions.put(cluster.getRegion(), group);
		});
	}

	static class GetConfigHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if ("GET".equals(exchange.getRequestMethod())) {
				String name = exchange.getRequestHeaders().getFirst("Name");
				String region = exchange.getRequestHeaders().getFirst("Region");
				String group = exchange.getRequestHeaders().getFirst("Group");
				new InstanceHandler().handleInstanceGet(exchange, name, region, group);
			} else {
				exchange.sendResponseHeaders(405, -1);
			}
		}
	}

	static class InstanceHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String method = exchange.getRequestMethod();
			if ("GET".equalsIgnoreCase(method)) {
				String name = getLastPartOfPath(exchange);
				handleInstanceGet(exchange, name, null, null);
			} else {
				sendResponse(exchange, 405, "Method Not Allowed");
			}
		}

		private void handleInstanceGet(HttpExchange exchange, String name, String regionName, String groupName)
				throws IOException {
			if ("instances".equalsIgnoreCase(name)) {
				sendResponse(exchange, 200, mapper.writeValueAsString(instances.values()));
			} else {
				if (name == null || null == regionName) {
					System.err.println("Name and Region are mandarory for new instance creation!");
					sendResponse(exchange, 404, "Name and Region are mandarory for new instance creation!");
					return;
				}
				Instance instance = instances.get(name);
				if (instance == null) {
					instance = createValidInstace(exchange, name, regionName, groupName);
					if (instance == null) {
						return;
					}
					instances.put(name, instance);
				}
				Group group = groups.get(instance.group);
				Cluster cluster = clusters.get(group.cluster);
				InstanceDetails instanceDetails = new InstanceDetails(instance, group, cluster);
				sendResponse(exchange, 200, mapper.writeValueAsString(instanceDetails));
			}
		}

		private Instance createValidInstace(HttpExchange exchange, String name, String regionName, String groupName)
				throws IOException {
			if (null == groupName || groupName.isBlank()) {
				if (null == regions.get(regionName)) {
					System.err.println("Region must be a valid existing Region!");
					sendResponse(exchange, 404, "Region must be a valid existing Region!");
					return null;
				}
				groupName = regions.get(regionName).name;
			} else {
				if (null == groups.get(groupName)) {
					System.err.println("Group must be a valid existing Group!");
					sendResponse(exchange, 404, "Group must be a valid existing Group!");
					return null;
				}
			}
			return new Instance(name, regionName, groupName);
		}
	}

	static class GroupHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String method = exchange.getRequestMethod();
			if ("GET".equalsIgnoreCase(method)) {
				sendResponse(exchange, 200, mapper.writeValueAsString(groups.values()));
			} else if ("POST".equalsIgnoreCase(method)) {
				Group group = mapper.readValue(exchange.getRequestBody(), Group.class);
				groups.put(group.name, group);
				updateDefaultRegion(group);
				sendResponse(exchange, 201, mapper.writeValueAsString(group));
			} else if ("PATCH".equalsIgnoreCase(method)) {
				String name = getLastPartOfPath(exchange);
				String newCluster = getQueryParam(exchange, "new-cluster");
				Group group = groups.get(name);
				if (group != null) {
					group.cluster = newCluster;
					sendResponse(exchange, 200, mapper.writeValueAsString(group));
				} else {
					System.err.println("Group not found");
					sendResponse(exchange, 404, "Group not found");
				}
			} else if ("DELETE".equalsIgnoreCase(method)) {
				String name = getLastPartOfPath(exchange);
				Group removed = groups.remove(name);
				if (removed != null) {
					sendResponse(exchange, 200, "Group deleted");
				} else {
					System.err.println("Group not found");
					sendResponse(exchange, 404, "Group not found");
				}
			} else {
				sendResponse(exchange, 405, "Method Not Allowed");
			}
		}

		private void updateDefaultRegion(Group group) {
			if (regions.get(clusters.get(group.cluster).getRegion()) == null) {
				regions.put(clusters.get(group.cluster).getRegion(), group);
			}
		}
	}

	static class ClusterHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
				sendResponse(exchange, 200, mapper.writeValueAsString(clusters.values()));
			} else {
				sendResponse(exchange, 405, "Method Not Allowed");
			}
		}
	}

	private static void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
		exchange.sendResponseHeaders(code, response.getBytes().length);
		OutputStream os = exchange.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}

	private static String getQueryParam(HttpExchange exchange, String key) {
		String query = exchange.getRequestURI().getQuery();
		if (query == null)
			return null;
		return Arrays.stream(query.split("&")).map(param -> param.split("="))
				.filter(pair -> pair.length == 2 && pair[0].equals(key)).map(pair -> pair[1]).findFirst().orElse(null);
	}

	private static String getLastPartOfPath(HttpExchange exchange) {
		String path = exchange.getRequestURI().getPath();
		return Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).reduce((s1, s2) -> s2).get();
	}

	static class Instance {
		public String name;
		public String region;
		public String group;

		public Instance() {
		}

		public Instance(String name, String region, String group) {
			this.name = name;
			this.region = region;
			this.group = group;
		}
	}

	static class Group {
		public String name;
		public String cluster;

		public Group() {
		}

		public Group(String name, String cluster) {
			this.name = name;
			this.cluster = cluster;
		}
	}

	static class InstanceDetails {
		public String instanceName;
		public String groupName;
		public String clusterName;
		public String serviceUrl;
		public String authPluginClassName;
		public boolean tlsHostnameVerificationEnable;
		public String authParamsString;
		public String region;

		public InstanceDetails(Instance instance, Group group, Cluster cluster) {
			this.instanceName = instance.name;
			this.groupName = group.name;
			this.clusterName = cluster.getName();
			this.serviceUrl = cluster.getServiceUrl();
			this.authPluginClassName = cluster.getAuthPluginClassName();
			this.tlsHostnameVerificationEnable = cluster.isTlsHostnameVerificationEnable();
			this.authParamsString = cluster.getAuthParamsString();
			this.region = cluster.getRegion();
		}
	}

}
