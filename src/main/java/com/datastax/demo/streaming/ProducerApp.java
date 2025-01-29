package com.datastax.demo.streaming;

import org.apache.pulsar.client.api.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class ProducerApp {

	private static final String SERVICE_URL = "<your pulsar service url>";
	private static final String PULSAR_AUTH_TOKEN = "<your pulsar token>";
	private static final String PULSAR_TOPIC_FULLPATH = "<your pulsar topic>";
	private static final int PRODUCING_DELAY_IN_MILLISECONDS = 500;

	public static void main(String[] args) throws IOException {
		PulsarClient client = PulsarClient.builder().serviceUrl(SERVICE_URL)
				.authentication(AuthenticationFactory.token(PULSAR_AUTH_TOKEN)).build();

		Producer<byte[]> producer = client.newProducer().topic(PULSAR_TOPIC_FULLPATH).create();
		System.out.println("Producer started!");

		IntStream.range(0, 100).forEach(i -> {
			try {
				String msg = "Hello Pulsar" + i;
				producer.send(msg.getBytes());
				System.out.printf("Sending msg: %s \n", msg);
				TimeUnit.MILLISECONDS.sleep(PRODUCING_DELAY_IN_MILLISECONDS);
			} catch (PulsarClientException | InterruptedException e) {
				e.printStackTrace();
			}
		});

		// Close Producer & client
		producer.close();
		client.close();
	}
}