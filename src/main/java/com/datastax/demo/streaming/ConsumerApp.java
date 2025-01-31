package com.datastax.demo.streaming;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.SubscriptionType;

public class ConsumerApp {

	public static void main(String[] args) throws IOException, InterruptedException {
		StreamUtil util = new StreamUtil();
		PulsarClient client = util.getClient();

		// Create consumer on a topic with a subscription
		Consumer<?> consumer = client.newConsumer().topic(util.PULSAR_TOPIC_FULLPATH)
				.subscriptionName(util.PULSAR_SUBSCRIPTION).subscriptionType(SubscriptionType.Exclusive)
				.subscribe();
		System.out.println("Consumer started using Pulsar service at " + util.SERVICE_URL);

		boolean receivedMsg;
		do { // Process message blacklog & wait CLIENT_WAIT_IN_SECONDS secs for new message
			receivedMsg = false;
			Message<?> msg = consumer.receive(util.CLIENT_WAIT_IN_SECONDS, TimeUnit.SECONDS);

			if (msg != null) {
				receivedMsg = true;
				System.out.printf("Consumed msg: %s at %s\n", new String(msg.getData()), util.getcurrentTime());

				consumer.acknowledge(msg); // Remove consumed msg from backlog
				TimeUnit.MILLISECONDS.sleep(util.CONSUMING_DELAY_IN_MILLISECONDS);
			}
		} while (receivedMsg);
		System.out.printf("Consumer: No new messages to process in the last %s seconds!\n",
				util.CLIENT_WAIT_IN_SECONDS);

		// Close Consumer & client
		System.out.println("Consumer Pulsar service at " + util.SERVICE_URL + " shuting down!");
		consumer.close();
		client.close();
	}
}