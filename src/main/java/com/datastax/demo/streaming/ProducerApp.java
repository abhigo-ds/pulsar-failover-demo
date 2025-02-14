package com.datastax.demo.streaming;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;

public class ProducerApp {

	public static void main(String[] args) throws IOException {
		StreamUtil util = new StreamUtil();
		PulsarClient client = util.getClient();
		Producer<byte[]> producer = client.newProducer().topic(util.getConfig().getTopicFullPath()).create();
		System.out.println("Producer started using Pulsar service at " + util.getCurrentServiceUrl());

        IntStream.iterate(1, n -> n + 1).forEach(i -> {
			try {
				String msg = "Message " + i + " at " + util.getcurrentTime() + " from " + util.getCurrentServiceUrl();
				producer.send(msg.getBytes());
				System.out.printf("Producing: %s \n", msg);
				TimeUnit.MILLISECONDS.sleep(util.getConfig().getProducerDelayMillis());
			} catch (PulsarClientException | InterruptedException e) {
				e.printStackTrace();
			}
		});

		// Close Producer & client
		System.out.println("Producer Pulsar service at " + util.getCurrentServiceUrl() + " shuting down!");
		producer.close();
		client.close();
	}

}