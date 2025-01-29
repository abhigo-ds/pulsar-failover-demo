package com.datastax.demo.streaming;

import org.apache.pulsar.client.api.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ConsumerApp {

	private static final String SERVICE_URL = "<your pulsar service url>";
	private static final String PULSAR_AUTH_TOKEN = "<your pulsar token>";
	private static final String PULSAR_TOPIC_FULLPATH = "<your pulsar topic>";
	private static final String PULSAR_SUBSCRIPTION = "<your pulsar topic subscription>";
	private static final int CLIENT_WAIT_IN_SECONDS = 10;
	private static final int CONSUMING_DELAY_IN_MILLISECONDS = 300;

	public static void main(String[] args) throws IOException, InterruptedException {
		PulsarClient client = PulsarClient.builder().serviceUrl(SERVICE_URL)
				.authentication(AuthenticationFactory.token(PULSAR_AUTH_TOKEN)).build();

		// Create consumer on a topic with a subscription
		Consumer<?> consumer = client.newConsumer().topic(PULSAR_TOPIC_FULLPATH).subscriptionName(PULSAR_SUBSCRIPTION)
				.subscriptionType(SubscriptionType.Exclusive).subscribe();
		System.out.println("Consumer started!");

		boolean receivedMsg = false;
		do { // Process message blacklog & wait upto 10 secs for new message
			receivedMsg = false;
			Message<?> msg = consumer.receive(CLIENT_WAIT_IN_SECONDS, TimeUnit.SECONDS);

			if (msg != null) {
				receivedMsg = true;
				System.out.printf("Consumed msg: %s \n", new String(msg.getData()));

				consumer.acknowledge(msg); // Remove consumed msg from backlog
				TimeUnit.MILLISECONDS.sleep(CONSUMING_DELAY_IN_MILLISECONDS);
			}
		} while (receivedMsg);
		System.out.println("Consumer: No new messages to process in the last 10 seconds, exiting!");

		// Close Consumer & client
		consumer.close();
		client.close();
	}
}