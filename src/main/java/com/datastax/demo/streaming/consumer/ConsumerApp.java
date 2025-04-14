package com.datastax.demo.streaming.consumer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.SubscriptionType;

import com.datastax.demo.streaming.StreamUtil;

public class ConsumerApp {

	/**
	 * This application initializes a new pulsar client using
	 * {@link org.apache.pulsar.client.impl.ControlledClusterFailover
	 * ControlledClusterFailover} and creates a Consumer application which is then
	 * used to consume messages from the pulsar topic.
	 *
	 * @param args CommandLine(CLI) arguments defining application characteristics.
	 *             <ol>
	 *             <li>{@code args[0]}
	 *             {@literal A name for the consumer to be started}
	 *             <li>{@code args[1]}
	 *             {@literal Name:Type, these are colon seperated values.
	 *                  Subscription Name and Type for the consumer. See below for the valid Subscription Types.}
	 *             <ol>
	 *             <li>{@literal S: }{@code Shared:} {@literal Default value. In case the Subscription Type value is wrong or unknown or not provided.}
	 *             <li>{@literal E: }{@code Exclusive}
	 *             <li>{@literal F: }{@code Failover}
	 *             </ol>
	 *             <li>{@code args[2]}
	 *             {@literal Region where this application is hosted/deployed}
	 *             <li>{@code args[3]}
	 *             {@literal Name of the group this app should belong to.
	 *                  This will help in determining the cluster where the pulsar client will connect.}
	 *             </ol>
	 * @throws IOException          Exception thrown from the app
	 * @throws InterruptedException Exception thrown from the app
	 * @implNote See below for the list of CLI required at the runtime.
	 * @see org.apache.pulsar.client.impl.ControlledClusterFailover
	 */
	public static void main(String[] args) throws IOException, InterruptedException {

		StreamUtil util = new StreamUtil(args, "Consumer");
		String subDtls = util.validateSubDtls(args[1]);
		String subName = subDtls.split(":")[0];
		String subType = subDtls.split(":")[1];
		PulsarClient client = util.getClient();

		// Create consumer on a topic with a subscription
		Consumer<?> consumer = client.newConsumer().consumerName(util.getName())
				.topic(util.getConfig().getTopicFullPath()).subscriptionName(subName).replicateSubscriptionState(true)
				.subscriptionType(SubscriptionType.valueOf(subType)).subscribe();
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
		System.out.println("Consumer Pulsar service at " + util.getCurrentServiceUrl() + " shutting down!");
		consumer.close();
		client.close();
	}
}