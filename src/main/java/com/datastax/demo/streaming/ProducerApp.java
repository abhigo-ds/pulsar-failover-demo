package com.datastax.demo.streaming;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;

public class ProducerApp {

	private static final String serviceURL = StreamUtil.SERVICE_URL_EAST1;

	public static void main(String[] args) throws IOException {
		PulsarClient client = PulsarClient.builder().serviceUrl(serviceURL)
				.authentication(AuthenticationFactory.token(StreamUtil.PULSAR_AUTH_TOKEN_EAST1)).build();

		Producer<byte[]> producer = client.newProducer().topic(StreamUtil.PULSAR_TOPIC_FULLPATH).create();
		System.out.println("Producer started using Pulsar service at " + serviceURL);

		IntStream.range(0, 100).forEach(i -> {
			try {
				String msg = "Message " + i + " " + StreamUtil.getcurrentTime();
				producer.send(msg.getBytes());
				System.out.printf("Sending msg: %s \n", msg);
				TimeUnit.MILLISECONDS.sleep(StreamUtil.PRODUCING_DELAY_IN_MILLISECONDS);
			} catch (PulsarClientException | InterruptedException e) {
				e.printStackTrace();
			}
		});

		// Close Producer & client
		System.out.println("Producer Pulsar service at " + serviceURL + " shuting down!");
		producer.close();
		client.close();
	}

}