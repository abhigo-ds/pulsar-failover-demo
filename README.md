# pulsar-failover-demo
A sample app that demos [Pulsar](https://pulsar.apache.org/ "Pulsar") client-side [Controlled failover](https://pulsar.apache.org/docs/3.3.x/client-libraries-cluster-level-failover/#controlled-failover "Controlled failover") 

This app has three components
- A [**Producer** app](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/java/com/datastax/demo/streaming/producer/ProducerApp.java "**Producer** app") that produce messages a regular intervals. You can start one of more instances of this app.
- A [**Consumer** app](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/java/com/datastax/demo/streaming/consumer/ConsumerApp.java "**Consumer** app") that consumes messages produced by the above Producer. You can start one of more instances of this app.
- A [**Provider** app](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/java/com/datastax/demo/streaming/provider/ClusterConfigProvider.java "**Provider** app") that works as a simple URL Service Provider as [shown here](https://pulsar.apache.org/docs/3.3.x/client-libraries-cluster-level-failover/#controlled-failover "shown here"). You must start only one instance of this app before you start the Producer or Consumer. If you plan to start more than one instance of this app for HA, you must put these instances behind a single HA load balancer.

Below is a high-level diagram of the above components and the Pulsar Controlled Failover flow

<img width="675" alt="cluster-level-failover-3-e4c1f0e86f1652f300f2bc54d342b955" src="src/main/resources/cluster-level-failover.png" />


------------

### Prerequisites
- Java 11
- Apache Maven 3.8.x (to build the app)
- Pulsar 3.x 
- Two (or more) Pulsar based Streaming clusters
 - One clusters will be the primary, while all the other clusters will be DR clusters that can takeover in case of a failover
 - For the purpose of this demo, we will use Astra Streaming (A SaaS Streaming provider by DataStax) to standup two Pulsar based streaming clusters.
 
 
### Building the app
From the root folder of the repo, run the below command
`mvn clean package -Passembly`

### Running the app
First start the **Provider** app using the below command
`java -cp target/producer_failover-*-jar-with-dependencies.jar com.datastax.demo.streaming.provider.ClusterConfigProvider`

Then start the **Producer** app using the below command
`java -cp target/producer_failover-*-jar-with-dependencies.jar com.datastax.demo.streaming.producer.ProducerApp`

Then start the **Consumer** app using the below command
`java -cp target/producer_failover-*-jar-with-dependencies.jar com.datastax.demo.streaming.consumer.ConsumerApp`


### Performing failover
The above demo app uses two SaaS cluster provided by Astra Streaming (ideally deployed in two different regions) with bidirectional replication. Internally it refers to them as **clusterA** and **clusterB**. 

By default, clusterA is chosen as the **Primary** and clusterB as **DR/Failover**. 

You can find which cluster is currently Primary anytime by hitting the Provider endpoint at `/getConfig`. If you are running the app locally, it can be access [here](http://localhost:8080/getconfig)

To inject a failover, go to Provider endpoint `/setConfig` and pass the url param `cluster-name` with a value of the `cluster-name` you want to failover to.
- If running locally, you could initiate a failover hitting endpoint `http://localhost:8080/setconfig?cluster-name=clusterB`
