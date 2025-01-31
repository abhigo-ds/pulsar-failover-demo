package com.datastax.demo.streaming;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.PulsarClient;

public class StreamUtil {
	final String SERVICE_URL_EAST1 = "<add your value>";
	final String SERVICE_URL_EAST4 = "<add your value>";
	final String PULSAR_AUTH_TOKEN_EAST1 = "<add your value>";
	final String PULSAR_AUTH_TOKEN_EAST4 = "<add your value>";
	final String PULSAR_TOPIC_FULLPATH = "<add your value>";
	final String PULSAR_SUBSCRIPTION = "<add your value>";
	
	final String SERVICE_URL = SERVICE_URL_EAST1;
	final int PRODUCING_DELAY_IN_MILLISECONDS = 500;
	final int CONSUMING_DELAY_IN_MILLISECONDS = 300;
	final int CLIENT_WAIT_IN_SECONDS = 60;
	
	private final DateTimeFormatter dtfDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	
	String getcurrentTime() {
		return dtfDateTime.format(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
	}
	
	public PulsarClient getClient() throws IOException {
		return PulsarClient.builder().serviceUrl(SERVICE_URL)
				.authentication(AuthenticationFactory.token(PULSAR_AUTH_TOKEN_EAST1)).build();
	}
	
}
