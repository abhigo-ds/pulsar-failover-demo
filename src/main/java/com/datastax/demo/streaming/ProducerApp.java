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
		Producer<byte[]> producer = client.newProducer().topic(util.PULSAR_TOPIC_FULLPATH).create();
		System.out.println("Producer started using Pulsar service at " + util.SERVICE_URL);

		IntStream.range(0, 100).forEach(i -> {
			try {
				String msg = "Message " + i + " " + util.getcurrentTime();
				producer.send(msg.getBytes());
				System.out.printf("Sending msg: %s \n", msg);
				TimeUnit.MILLISECONDS.sleep(util.PRODUCING_DELAY_IN_MILLISECONDS);
			} catch (PulsarClientException | InterruptedException e) {
				e.printStackTrace();
			}
		});

		// Close Producer & client
		System.out.println("Producer Pulsar service at " + util.SERVICE_URL + " shuting down!");
		producer.close();
		client.close();
	}

}