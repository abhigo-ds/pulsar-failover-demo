package com.datastax.demo.streaming.consumer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.SubscriptionType;

import com.datastax.demo.streaming.StreamUtil;

public class ConsumerApp {

	public static void main(String[] args) throws IOException, InterruptedException {
		StreamUtil util = new StreamUtil();
		PulsarClient client = util.getClient();

		// Create consumer on a topic with a subscription
		Consumer<?> consumer = client.newConsumer().topic(util.getConfig().getTopicFullPath())
				.subscriptionName(util.getConfig().getSubscription()).replicateSubscriptionState(true)
				.subscriptionType(SubscriptionType.Exclusive).subscribe();
		System.out.println("Consumer started using Pulsar service at " + util.getCurrentServiceUrl());

		boolean receivedMsg;
		do { // Process message blacklog & wait CLIENT_WAIT_IN_SECONDS secs for new message
			receivedMsg = false;
			Message<?> msg = consumer.receive(util.getConfig().getConsumerWaitSeconds(), TimeUnit.SECONDS);

			if (msg != null) {
				receivedMsg = true;
				System.out.printf("Consuming: %s at %s on %s\n", new String(msg.getData()), util.getcurrentTime(),
						util.getCurrentServiceUrl());

				consumer.acknowledge(msg); // Remove consumed msg from backlog
				TimeUnit.MILLISECONDS.sleep(util.getConfig().getConsumerDelayMillis());
			}
		} while (receivedMsg);
		System.out.printf("Consumer: No new messages to process in the last %s seconds!\n",
				util.getConfig().getConsumerWaitSeconds());

		// Close Consumer & client
		System.out.println("Consumer Pulsar service at " + util.getCurrentServiceUrl() + " shuting down!");
		consumer.close();
		client.close();
	}
}