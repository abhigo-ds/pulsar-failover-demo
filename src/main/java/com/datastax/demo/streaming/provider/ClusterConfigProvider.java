package com.datastax.demo.streaming.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.datastax.demo.streaming.Cluster;
import com.datastax.demo.streaming.StreamConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ClusterConfigProvider {

	private static final Map<String, Cluster> configMap = new HashMap<>();
	private static Cluster currentConfig;

	public static void main(String[] args) throws Exception {
		StreamConfig config = new StreamConfig();
		config.getClusters().forEach(cluster -> configMap.put(cluster.getName(), cluster));
		currentConfig = config.getDefaultCluster();

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
			if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
				// Read the entire POST request body as a string (assumes UTF-8 encoding)
				String body = new String(exchange.getRequestBody().readAllBytes(), "UTF-8");
				Map<String, String> params = jsonToMap(body);
				String clusterName = params.get("cluster-name");
				String response;

				if (clusterName == null || clusterName.isEmpty()) {
					exchange.sendResponseHeaders(400, 0);
					response = "Missing 'cluster-name' parameter.";
				} else if (!configMap.containsKey(clusterName)) {
					exchange.sendResponseHeaders(404, 0);
					response = "Config '" + clusterName + "' not found.";
				} else {
					// Set the current configuration
					currentConfig = configMap.get(clusterName);
					exchange.sendResponseHeaders(200, 0);
					response = "Current config set to '" + clusterName + "' (" + currentConfig.getServiceUrl() + ").";
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
				if (currentConfig == null) {
					String response = "No config set. Use /setconfig?config-name=xxx to set a config.";
					exchange.sendResponseHeaders(404, response.getBytes().length);
					try (OutputStream os = exchange.getResponseBody()) {
						os.write(response.getBytes());
					}
				} else {
					String jsonResponse = currentConfig.toJson();
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

	private static Map<String, String> jsonToMap(String json) {
		Map<String, String> result = new HashMap<>();
		if (json == null || json.trim().isEmpty()) {
			return result;
		}
		JSONObject jsonObject = new JSONObject(json);
		for (String key : jsonObject.keySet()) {
			result.put(key, jsonObject.get(key).toString());
		}
		return result;
	}

}
