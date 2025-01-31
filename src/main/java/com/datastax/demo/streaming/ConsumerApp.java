package com.datastax.demo.streaming;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.SubscriptionType;

public class ConsumerApp {

	private static final String serviceURL = StreamUtil.SERVICE_URL_EAST1;

	public static void main(String[] args) throws IOException, InterruptedException {
		PulsarClient client = PulsarClient.builder().serviceUrl(serviceURL)
				.authentication(AuthenticationFactory.token(StreamUtil.PULSAR_AUTH_TOKEN_EAST1)).build();

		// Create consumer on a topic with a subscription
		Consumer<?> consumer = client.newConsumer().topic(StreamUtil.PULSAR_TOPIC_FULLPATH)
				.subscriptionName(StreamUtil.PULSAR_SUBSCRIPTION).subscriptionType(SubscriptionType.Exclusive)
				.subscribe();
		System.out.println("Consumer started using Pulsar service at " + serviceURL);

		boolean receivedMsg;
		do { // Process message blacklog & wait CLIENT_WAIT_IN_SECONDS secs for new message
			receivedMsg = false;
			Message<?> msg = consumer.receive(StreamUtil.CLIENT_WAIT_IN_SECONDS, TimeUnit.SECONDS);

			if (msg != null) {
				receivedMsg = true;
				System.out.printf("Consumed msg: %s at %s\n", new String(msg.getData()), StreamUtil.getcurrentTime());

				consumer.acknowledge(msg); // Remove consumed msg from backlog
				TimeUnit.MILLISECONDS.sleep(StreamUtil.CONSUMING_DELAY_IN_MILLISECONDS);
			}
		} while (receivedMsg);
		System.out.printf("Consumer: No new messages to process in the last %s seconds!\n",
				StreamUtil.CLIENT_WAIT_IN_SECONDS);

		// Close Consumer & client
		System.out.println("Consumer Pulsar service at " + serviceURL + " shuting down!");
		consumer.close();
		client.close();
	}
}