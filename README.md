# pulsar-failover-demo
A sample app that demos [Pulsar](https://pulsar.apache.org/ "Pulsar") client-side [Controlled failover](https://pulsar.apache.org/docs/3.3.x/client-libraries-cluster-level-failover/#controlled-failover "Controlled failover") 

This app has three components
- A [**Producer** app](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/java/com/datastax/demo/streaming/producer/ProducerApp.java "**Producer** app") that produce messages a regular intervals. You can start one of more instances of this app.
- A [**Consumer** app](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/java/com/datastax/demo/streaming/consumer/ConsumerApp.java "**Consumer** app") that consumes messages produced by the above Producer. You can start one of more instances of this app.
- A [**Provider** app](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/java/com/datastax/demo/streaming/provider/ClusterConfigProvider.java "**Provider** app") that works as a simple URL Service Provider as [shown here](https://pulsar.apache.org/docs/3.3.x/client-libraries-cluster-level-failover/#controlled-failover "shown here"). You must start only one instance of this app before you start the Producer or Consumer. If you plan to start more than one instance of this app for HA, you must put these instances behind a single HA load balancer.

Below is a high-level diagram of the above components and the Pulsar Controlled Failover flow

<img width="675" alt="cluster-level-failover-3-e4c1f0e86f1652f300f2bc54d342b955" src="https://github.com/user-attachments/assets/5a8ddeeb-8945-4378-b232-1b842ea4ea95" />
