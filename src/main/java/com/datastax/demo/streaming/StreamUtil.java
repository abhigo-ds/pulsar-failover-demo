package com.datastax.demo.streaming;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.ServiceUrlProvider;
import org.apache.pulsar.client.impl.ControlledClusterFailover;

public class StreamUtil {
	private StreamConfig config = null;
	private ServiceUrlProvider provider = null;
	private PulsarClient client = null;
	private DateTimeFormatter dtfDateTime = null;

	public String getcurrentTime() {
		return dtfDateTime.format(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
	}

	public StreamUtil() {
		config = new StreamConfig();
		// Initialize Provider & Client
		getControlledFailoverClient();
		try {
			TimeUnit.SECONDS.sleep(6);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		dtfDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	}

	public PulsarClient getClient() {
		return client;
	}

	public PulsarClient getControlledFailoverClient() {
		try {
			provider = ControlledClusterFailover.builder().defaultServiceUrl(config.getServiceUrlDefault())
					.checkInterval(5, TimeUnit.SECONDS).urlProvider(config.getProviderUrl()).build();
			client = PulsarClient.builder().serviceUrlProvider(provider).build();
			provider.initialize(client);
		} catch (IOException e) {
			e.printStackTrace();
			try {
				client = PulsarClient.builder().serviceUrl(config.getServiceUrlDefault())
						.authentication(AuthenticationFactory.token(config.getAuthTokenDefault())).build();
			} catch (PulsarClientException e1) {
				e1.printStackTrace();
			}
		}

		return client;
	}

	public StreamConfig getConfig() {
		return config;
	}

	public String getCurrentServiceUrl() {
		int schemeEnd = provider.getServiceUrl().indexOf("://");
		if (schemeEnd == -1) {
			throw new IllegalArgumentException("Invalid URL format: missing '://'");
		}

		// Get the part after the scheme, e.g.,
		// "pulsar-gcp-useast1.streaming.datastax.com:6651"
		String remainder = provider.getServiceUrl().substring(schemeEnd + 3);

		// Remove the port part if it exists (after the colon)
		int colonIndex = remainder.indexOf(':');
		if (colonIndex != -1) {
			remainder = remainder.substring(0, colonIndex);
		}

		// Extract the first part of the domain (up to the first period)
		int dotIndex = remainder.indexOf('.');
		if (dotIndex != -1) {
			return remainder.substring(0, dotIndex);
		}

		// If no period is found, return the whole remainder
		return remainder;
	}

}
