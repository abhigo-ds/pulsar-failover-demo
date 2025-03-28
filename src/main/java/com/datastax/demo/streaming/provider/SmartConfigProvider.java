package com.datastax.demo.streaming.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.datastax.demo.streaming.Cluster;
import com.datastax.demo.streaming.StreamConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class SmartConfigProvider {

	private static final Map<String, Instance> instances = new ConcurrentHashMap<>();
	private static final Map<String, Group> groups = new ConcurrentHashMap<>();
	private static final Map<String, Cluster> clusters = new ConcurrentHashMap<>();

	private static final ObjectMapper mapper = new ObjectMapper();

	public static void main(String[] args) throws IOException {
		StreamConfig config = new StreamConfig();
		config.getClusters().forEach(cluster -> clusters.put(cluster.getName(), cluster));

		HttpServer server = HttpServer.create(new InetSocketAddress(config.getProviderPort()), 0);
		server.createContext("/getconfig", new GetConfigHandler());
		server.createContext("/instances", new InstanceHandler());
		server.createContext("/groups", new GroupHandler());
		server.createContext("/clusters", new ClusterHandler());

		server.setExecutor(null);
		server.start();
		System.out.println("SmartConfigProvider started at port " + config.getProviderPort());
	}

	static class GetConfigHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if ("GET".equals(exchange.getRequestMethod())) {
				String name = exchange.getRequestHeaders().getFirst("Instance");
				new InstanceHandler().handleInstanceGet(exchange, name);
			} else {
				exchange.sendResponseHeaders(405, -1); // Method Not Allowed
			}
		}
	}

	static class InstanceHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String method = exchange.getRequestMethod();
			if ("GET".equalsIgnoreCase(method)) {
				String name = getLastPartOfPath(exchange);
				handleInstanceGet(exchange, name);
			} else if ("POST".equalsIgnoreCase(method)) {
				Instance instance = mapper.readValue(exchange.getRequestBody(), Instance.class);
				instances.put(instance.name, instance);
				sendResponse(exchange, 201, mapper.writeValueAsString(instance));
			} else if ("PATCH".equalsIgnoreCase(method)) {
				String name = getLastPartOfPath(exchange);
				String newGroup = getQueryParam(exchange, "new-group");
				Instance instance = instances.get(name);
				if (instance != null) {
					instance.group = newGroup;
					sendResponse(exchange, 200, mapper.writeValueAsString(instance));
				} else {
					sendResponse(exchange, 404, "Instance not found");
				}
			} else {
				sendResponse(exchange, 405, "Method Not Allowed");
			}
		}

		private void handleInstanceGet(HttpExchange exchange, String name) throws IOException {
			if ("instances".equalsIgnoreCase(name)) {
				sendResponse(exchange, 200, mapper.writeValueAsString(instances.values()));
			} else {
				Instance instance = instances.get(name);
				if (instance != null) {
					Group group = groups.get(instance.group);
					Cluster cluster = clusters.get(group.cluster);
					InstanceDetails instanceDetails = new InstanceDetails(instance, group, cluster);
					sendResponse(exchange, 200, mapper.writeValueAsString(instanceDetails));
				} else {
					sendResponse(exchange, 404, "Instance not found");
				}
			}
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
				sendResponse(exchange, 201, mapper.writeValueAsString(group));
			} else if ("PATCH".equalsIgnoreCase(method)) {
				String name = getLastPartOfPath(exchange);
				String newCluster = getQueryParam(exchange, "new-cluster");
				Group group = groups.get(name);
				if (group != null) {
					group.cluster = newCluster;
					sendResponse(exchange, 200, mapper.writeValueAsString(group));
				} else {
					sendResponse(exchange, 404, "Group not found");
				}
			} else if ("DELETE".equalsIgnoreCase(method)) {
				String name = getLastPartOfPath(exchange);
				Group removed = groups.remove(name);
				if (removed != null) {
					sendResponse(exchange, 200, "Group deleted");
				} else {
					sendResponse(exchange, 404, "Group not found");
				}
			} else {
				sendResponse(exchange, 405, "Method Not Allowed");
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
	}

	static class Group {
		public String name;
		public String cluster;
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
