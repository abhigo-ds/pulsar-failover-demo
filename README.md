# pulsar-failover-demo
A sample app that demos [Pulsar](https://pulsar.apache.org/ "Pulsar") dynamic client (Producer/Consumer) traffic routing using a REST based `Provider` app that generates [pulsar-native configuration](https://pulsar.apache.org/docs/3.3.x/client-libraries-cluster-level-failover/#controlled-failover "Controlled failover") to manage which Pulsar cluster the clients should dynamically connect to in real-time. Note, the `Provider` app can be used in two ways
 - **Simple Provider**: This Provider allows `Fixed` routing. Only one cluster will be `Active` at any given time, and all other clusters will be set as `Passive`. All clients (Producer/Consumer) will connect to the `Active` cluster
 - **Smart Provider**: This Provider allows `Smart and Dynamic` routing.Any Pulsar client (Producer/Consumer) can be mapped to any cluster in real-time based on routing managed by the `Provider`. This is an `Active/Active` setup.
 

This app has Four components
- A [**Simple Provider** app](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/java/com/datastax/demo/streaming/provider/ClusterConfigProvider.java ) that works as a simple URL Service Provider as [shown here](https://pulsar.apache.org/docs/3.3.x/client-libraries-cluster-level-failover/#controlled-failover "shown here"). 
- A [**Smart Provider** app](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/java/com/datastax/demo/streaming/provider/SmartConfigProvider.java) that is a heavily customized version of the above mentioned Pulsar cluster-level-failover technique to provide smart-routing based on user-defined-configurations that can be dynamically updated.
- A [**Producer** app](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/java/com/datastax/demo/streaming/producer/ProducerApp.java "**Producer** app") that produce messages at regular intervals. You can start one of more instances of this app.
- A [**Consumer** app](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/java/com/datastax/demo/streaming/consumer/ConsumerApp.java "**Consumer** app") that consumes messages produced by the above Producer. You can start one of more instances of this app.

> [!NOTE]
> You must start one (and only one) instance of the `Provider` app (either `Simple Provider` or the `Smart Provider`) before starting any clients (i.e. `Producer` or `Consumer`). If you plan on having more than one instance of this app for High-Availability (HA), you must put these instances behind a single HA load-balancer and the Producer and Consumer will need to access it via the load-balancer. You will also need to customize the app further such that all the instances share the same configuration state at any time (e.g. by using a shared DB to persist configuration).

Below is a high-level diagram of the above components and the Pulsar Controlled Failover flow

<img width="675" src="src/main/resources/images/cluster-level-failover.png" />


------------

### Prerequisites
- Java 11
- Apache Maven 3.8.x (to build the app)
- Pulsar 3.x 
- Two (or more) Pulsar based Streaming clusters
  - One clusters will be the **Primary**, while all the other clusters will be **DR** clusters that can takeover in case of a failover
  - For the purpose of this demo, we will use [Astra Streaming](https://www.datastax.com/products/astra-streaming) (SaaS Streaming provider by DataStax) to standup two Pulsar based streaming clusters.
 
 
### Building the app
- Update applicable properties of the [application.properties](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/resources/application.properties) file based on your Pulsar environment and use-case
- From root of this repo, run command `mvn clean package -Passembly`

### Running the app
First start either the **Simple Provider** or the **Smart Provider** app using command 
 - Simple Provider: `java -cp target/producer_failover-*-jar-with-dependencies.jar com.datastax.demo.streaming.provider.ClusterConfigProvider` 
 - Smart Provider: `java -cp target/producer_failover-*-jar-with-dependencies.jar com.datastax.demo.streaming.provider.SmartConfigProvider`

Then start the **Producer** app using command `java -cp target/producer_failover-*-jar-with-dependencies.jar com.datastax.demo.streaming.producer.ProducerApp  <unique-client-name> <region>` 

Finally start the **Consumer** app using command `java -cp target/producer_failover-*-jar-with-dependencies.jar com.datastax.demo.streaming.consumer.ConsumerApp  <unique-client-name> <Subscription(Name:Type)> <region>` 

> [!NOTE]
> - You can have as many (one or more) instances of `Producers` and `Consumer`, however you must have only one instance of `Provider`.  
> - The command-line params `<unique-client-name>` and `<region>` are **_only_** applicable when using the `Smart Provider`  
> - Consumer requires additional command-line params to define the Subscription details where the consumer will attach itself please see below for details. 
> - Subscription details param consists of two component which are ":" seperated.  
>   - Subscription Name:Subscription Type  
>     - Subscription Name: This will be the subscription name that the consumer will attach itself to. **_If the subscription name does not exist, a new subscription will be created_** 
>     - Subscription Type: Subscription type will determine the characteristics of the subscription and how the consumer behaves with the said subscription. 
>      There are different types of subscription and user will have control over which one to select from. Below are the details on different types of subscription.
>       - `S : Shared`
>         - A shared subscription allows multiple consumers to consume messages from a single topic in a round-robin fashion. 
>         - Subscription type Shared is treated as a default by this app. In case of the subscription type is not provided or is invalid/unknown the app will default to Shared subscription type. 
>      - `E : Exclusive`
>        - An exclusive subscription describes a basic publish-subscribe (pub-sub) pattern where a single consumer subscribes to a single topic and consumes from it.
>        - If a subscription is marked as Exclusive, the system is restricted to a single consumer, request to bring up any additional consumer will result in error.
>      - `F : Failover`
>        - In failover subscriptions, Pulsar designates one primary consumer and multiple standby consumers. If the primary consumer disconnects, the standby consumers begin consuming the subsequent unacknowledged messages.
>        - This requires multi consumer setup to achieve primary-standby dynamic.
>      - `K : Key shared`
>        - Key shared subscriptions allow multiple consumers to subscribe to a topic, and provide additional metadata in the form of keys that link messages to specific consumers.
>        - For the demo purpose of this app we will not be considering this subscription type.


### Performing failover using Simple Provider
The above demo app uses two [Astra Streaming](https://www.datastax.com/products/astra-streaming) SaaS clusters (ideally deployed in different regions) with bidirectional replication. [You can configure](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/resources/application.properties#L2) as many cluster as needed for your use-case. The `Simple Provider` will then allow you to mark any one cluster to be `Active` at anytime. All other cluster will be auto set to `Passive`.

You can find which cluster is currently Primary at anytime by hitting the Provider endpoint at `/getConfig`. If you are running the app locally, it can be accessed [here](http://localhost:8080/getconfig) `http://localhost:8080/getconfig`

To inject a failover, go to Provider endpoint `/setConfig` and pass a JSON request body of `{"cluster-name": "clusterB"}`. Note, the value can be `clusterA` OR `clusterB` based on the cluster you want to failover to.  If running locally, you could initiate a failover by posting the above JSON body to [endpoint](http://localhost:8080/setconfig) `http://localhost:8080/setconfig`

------------


### Simple Provider Demo 
- Start `Provider`, followed by `Producer` and `Consumer`. Producer and Consumer messages show `clusterA` (useast1) as the primary (see below screenshots)

  <img width="900" src="src/main/resources/images/ProviderStart.png" />
  
  <img width="900" src="src/main/resources/images/ProducerStart.png" />
  
  <img width="900" src="src/main/resources/images/ConsumerStart.png" />


- On `Provider`, check the current Cluster via a tool like Postname (see below screenshots)

  <img width="900" src="src/main/resources/images/ProviderGetConfig.png" />


- On `Provider`, now perform failover to `clusterB`

  <img width="900" src="src/main/resources/images/ProviderSetConfig.png" />


- Now `Producer` and `Consumer` messages should show `clusterB` (useast4) post failover

  <img width="675" src="src/main/resources/images/ProducerFailover.png" />
  
  <img width="900" src="src/main/resources/images/ConsumerFailover.png" />


------------

### Performing dynamic routing using Smart Provider
The above demo app uses two [Astra Streaming](https://www.datastax.com/products/astra-streaming) SaaS clusters (ideally deployed in different regions) with bidirectional replication. [You can configure](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/resources/application.properties#L2) as many cluster as needed for your use-case. The `Smart Provider` will then allow you to map any group of clients to any cluster as needed. All clusters are always `Active` at all times and any number of clients can be mapped to them.

For more details on how to use the `Smart Provider` for traffic routing, use the provided [REST OpenAPI specification file](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/resources/provider_api.yaml) and/or the sample [POSTMan Collection](https://github.com/datastax/pulsar-failover-demo/blob/main/src/main/resources/PulsarProvider.postman_collection.json) file.

------------
