package com.datastax.demo.streaming.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.datastax.demo.streaming.StreamConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ClusterConfigProvider {

	private static final Map<String, ClusterConfig> configMap = new HashMap<>();
	private static String currentConfigName;

	public static void main(String[] args) throws Exception {
		StreamConfig config = new StreamConfig();

		List<String> clusters = config.getClusters();
		currentConfigName = config.getClusterDefault();
		clusters.forEach(cluster -> configMap.put(cluster,
				new ClusterConfig(config.getServiceUrl(cluster), config.getAuthToken(cluster))));

		// Create HTTP service with REST endpoints
		HttpServer server = HttpServer.create(new InetSocketAddress(config.getProviderPort()), 0);
		server.createContext("/setconfig", new SetConfigHandler());
		server.createContext("/getconfig", new GetConfigHandler());

		server.setExecutor(null); // use the default executor
		server.start();
		System.out.println("ClusterConfigProvider started at port " + config.getProviderPort());
	}

	// Handle /setconfig endpoint
	static class SetConfigHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if ("GET".equals(exchange.getRequestMethod())) {
				// Parse query parameters from the URL
				Map<String, String> queryParams = queryToMap(exchange.getRequestURI().getQuery());
				String clusterName = queryParams.get("cluster-name");
				String response;

				if (clusterName == null || clusterName.isEmpty()) {
					exchange.sendResponseHeaders(400, 0);
					response = "Missing 'config-name' parameter.";
				} else if (!configMap.containsKey(clusterName)) {
					exchange.sendResponseHeaders(404, 0);
					response = "Config '" + clusterName + "' not found.";
				} else {
					// Set the current configuration
					currentConfigName = clusterName;
					ClusterConfig config = configMap.get(currentConfigName);
					exchange.sendResponseHeaders(200, 0);
					response = "Current config set to '" + clusterName + "' (" + config.serviceUrl + ").";
				}
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response.getBytes());
				}
			} else {
				exchange.sendResponseHeaders(405, -1); // Method Not Allowed
			}
		}
	}

	// Handle /getconfig endpoint
	static class GetConfigHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if ("GET".equals(exchange.getRequestMethod())) {
				if (currentConfigName == null) {
					String response = "No config set. Use /setconfig?config-name=xxx to set a config.";
					exchange.sendResponseHeaders(404, response.getBytes().length);
					try (OutputStream os = exchange.getResponseBody()) {
						os.write(response.getBytes());
					}
				} else {
					ClusterConfig config = configMap.get(currentConfigName);
					String jsonResponse = config.toJson();
					exchange.getResponseHeaders().add("Content-Type", "application/json");
					exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
					try (OutputStream os = exchange.getResponseBody()) {
						os.write(jsonResponse.getBytes());
					}
				}
			} else {
				exchange.sendResponseHeaders(405, -1); // Method Not Allowed
			}
		}
	}

	// Utility method to parse query parameters from a query string
	private static Map<String, String> queryToMap(String query) {
		Map<String, String> result = new HashMap<>();
		if (query == null) {
			return result;
		}
		String[] params = query.split("&");
		for (String param : params) {
			String[] entry = param.split("=");
			if (entry.length > 1) {
				result.put(entry[0], entry[1]);
			} else {
				result.put(entry[0], "");
			}
		}
		return result;
	}

	// Config POJO
	static class ClusterConfig {
		private final String serviceUrl;
		private final String authToken;
		private final String authPluginClassName = "org.apache.pulsar.client.impl.auth.AuthenticationToken";

		public ClusterConfig(String serviceUrl, String authToken) {
			this.serviceUrl = serviceUrl;
			this.authToken = authToken;
		}

		public String toJson() {
			return String.format(
					"{" + "\"serviceUrl\":\"%s\", " + "\"authPluginClassName\":\"%s\","
							+ "\"tlsHostnameVerificationEnable\":\"true\"," + "\"authParamsString\":\"%s\"}",
					serviceUrl, authPluginClassName, authToken);
		}
	}
}
