# dseGraphFrameLoad: DSE Graph Frames Loader

This project walks through how to load graph data using DSE Graph Frames with Spark. The following topics are covered: 
1. How to run this code
2. Create a Graph Model 
3. Creating the Vertex DataSet
4. Creating the Edge DataSet
5. Using MlLib to add features to the graph 

### DSE Graph Frames - Background

The [DSE Graph Frames] package is a Scala/Java API written to better integrate with DSE Graph. Navigate to the link to learn more. 

[DSE Graph Frames]: <https://www.datastax.com/dev/blog/dse-graph-frame>

## A. Set-Up 

***1. Pre Requisites***

This project was designed for use with DSE 6.0. If the DSE 6.0 jars are not yet publically available, you will have to follow the instructions in [this DSE 6.0 EAP installation guide,] located in this repository in the resources directory. 

[dse-eap6-dep.txt]: <https://github.com/pmehra7/dseGraphFrameLoad/blob/master/src/main/resources/dse-eap6-dep.txt>

Moving forward, this README assumes you have successfully set up DSE 6.0. 

***2. Build***

Clone this repository on a machine in an environment you have cloning access:

```sh
$ git clone git@github.com:pmehra7/dseGraphFrameLoad.git
```

Navigate to the parent directory and build this project:
```sh
$ cd dseGraphFrameLoad/
$ mvn clean package
```

***3. Download Data***

Download the data from Kaggle: https://www.kaggle.com/c/acquire-valued-shoppers-challenge/data and download the following files: 

```sh
transactions.csv
offers.csv 
trainHistory.csv
```

## B. How to Run 

***1. Start DSE***

1. In a new terminal window, navigate to your installation of DSE 6.0
```sh
$ cd ~/path/to/dse-6.0.0/
```

***2. Load Data into DSEFS***
 
```sh
$ dse fs
$ mkdir data
$ put /path/to/transactions.csv /data/
$ put /path/to/offers.csv /data/
$ put /path/to/trainHistory.csv /data/
```

3. Start DSE with graph, search, and spark enabled:
```sh
$ ./bin/dse cassandra -k -s -g
```

***3. Create Graph Schema***

*****3.a: Gremlin Console*****

1. In a new terminal window, navigate to your installation of DSE 6.0
```sh
$ cd ~/path/to/dse-6.0.0/
```

2. Start DSE with graph, search, and spark enabled:
```sh
$ ./bin/dse cassandra -k -s -g
```

3. Start the Gremlin console using the `dse` command and passing the additional command `gremlin-console`:
```sh
$ bin/dse gremlin-console
         \,,,/
         (o o)
-----oOOo-(3)-oOOo-----
plugin activated: tinkerpop.server
plugin activated: tinkerpop.utilities
plugin activated: tinkerpop.tinkergraph
gremlin>
```

4. [Create a new graph] to store the data and alias a graph traversal to run queries. If you are reusing a graph that you previously created, [drop the graph schema and data].

```sh
gremlin> system.graph('food').create()
==>null
```

[Create a new graph]:<https://docs.datastax.com/en/dse/5.1/dse-dev/datastax_enterprise/graph/using/createGraphGremlin.html> 

[drop the graph schema and data]:<https://docs.datastax.com/en/dse/5.1/dse-dev/datastax_enterprise/graph/using/dropSchemaDataGremlin.html>

5. On the remote Gremlin Server, set the timeout value to max. Use this setting to ensure that script processing will complete.

```sh
gremlin> :remote config timeout max 
```

6. Run `schema.groovy` from the resources directory to create the graph schema. 
```sh
gremlin> :load /path/to/dseGraphFrameLoad/src/main/resources/schema.groovy
```

*****3.b: DSE Studio Notebook*****

Instead of using the gremlin console, you can:
1. Install and open a [DSE Studio Notebook]
2. Create a [new graph configuration through the Studio UI]
3. Copy and paste the schema creation statements from the [schema.groovy] file into a studio cell
4. Execute the code against the server from studio

[DSE Studio Notebook]:<https://docs.datastax.com/en/dse/5.1/dse-dev/datastax_enterprise/studio/studioGettingStarted.html>

[new graph configuration through the Studio UI]:<https://docs.datastax.com/en/dse/5.1/dse-dev/datastax_enterprise/studio/createConnectionNotebook.html>

[schema.groovy]:<https://github.com/pmehra7/dseGraphFrameLoad/blob/master/src/main/resources/schema.groovy>


***4. Run Spark Job***

The spark job reads the downloaded Kaggle data files from DSEFS, builds the required data frames, and loads the data into DSE Graph via the DataStax GraphFrames. To understand how to use DSE GraphFrames, please read [DSE Graph Frames].

Submit the spark job with: 

```sh
$ dse spark-submit --class com.spark.graphframes.App dseGraphFrames-1.0-SNAPSHOT.jar
```

I used the following spark submit parameters on m4.4xlarge machines: 
```sh
$ dse spark-submit --class com.spark.graphframes.App dseGraphFrames-1.0-SNAPSHOT.jar --executor-memory=22G
```

### C. Graph Model

***1. Schema Description***

Here is a diagram showing the graph's schema:

<p align="center">
    <img src="https://image.ibb.co/gbU1En/schema_view.png" alt="image" width="40%">
</p>

Bold: Partition Key

Italic: Clustering Column


***2. Vertices:***

Each vertex label is a column in this table; the properties avaiable on the vertex are indicated via the rows.
|Product|Customer|Store|Offer|
|-------|-------|-------|-------|
|**chain**|**customer_id**|**chain**|**offer**|
|**company**| |market|category|
|**brand**| | |quantity|
|*dept*| | |company|
|*category*| | |offervalue|
|productsize| | |brand|
|productmeasure|
|purchasequantity|
|purchaseamount|
|customer_id|
|date|


***3. Edges:***
Each edge label is a column in this table; the properties avaiable on the vertex are indicated via the rows.
|visits|offer_used|purchases|
|--------------|--------------|--------------|
|date|date|date|
| |repeater|purchasequantity|
| |repeattrips|purchaseamount|


### D. Vertices DataFrame

The first key to understanding how to use DSE Graph Frames is to examine the vertex data frame. A vertex data frame requires a column called `~label` all parts of the primary key to be present as columns in the data frame.

For example, to create a data frame for `offers` with the desired columns from the `offer.csv` file:

For example: 
```scala
    val offers = offer.select(
      col("offer") as "offer",
      col("category"),
      col("quantity"),
      col("company"),
      col("offervalue"),
      col("brand")
    ).withColumn("~label", lit("offer"))
```

Here we created a data frame for the **offer vertex** with the label `~offer`. Notice how this matches what is defined in your schema: 

<p align="center">
    <img src="https://image.ibb.co/cjEq77/label_description.png" alt="image" width="40%">
</p>

### E. Edges DataFrame

The trickiest key to understanding how to use DSE Graph Frames is to examine the edge data frame. An edge data frame requires:

1. A column called `~label`
2. A column called `src`
3. A column called `dst`

To create an Edge DataSet for Customer --> Store, we start by extracting following columns from `transactions.csv`: srcLabel, dstLabel, edgeLabel. 

srcLabel: the label of the originating vertex (in this schema it is customer)
dstLabel: the label of the destination vertex (in this schema it is store)
edgeLabel: the label of the edge (in this schema it is visits)

For example: 

```scala
    val txEdge = transactions.withColumn("srcLabel", lit("customer"))
    .withColumnRenamed("id", "customer_id")
    .withColumn("dstLabel", lit("store"))
    .withColumn("edgeLabel", lit("visits"))
```

Here we create the edge DataSet that will be written to the graph. We use a DSE GraphFrames method that takes the srcLabel and primary key values of the source vertex, Customer, to create the  src value for the given edge. The same is done for the dstLabel. After, we select the edge label and the edge property columns. 

```scala
    val custToStore = txEdge.select(
    g.idColumn(col("srcLabel"), col("customer_id")) as "src", 
    g.idColumn(col("dstLabel"), col("chain")) as "dst", 
    col("edgeLabel") as "~label", 
    col("date") as "date")
```

### Examples

Let's run through this example for customer 86246. In the spark repl:

```sh
$ /navigate/to/dse6.0/
$ dse spark
Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
   /___/ .__/\_,_/_/ /_/\_\   version 2.2.0.10
      /_/

Using Scala version 2.11.8 (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_144)
Type in expressions to have them evaluated.
Type :help for more information.

scala> custToStore.select("src").limit(1) 
```
The result is some seeminly arbitary value like: `*customer:AAAACTEwMzk4NTI0Ng==*.` This is a Spark/DSE Graph Frames `id` that is created based off of the `customer_e` primary key. The `dst` value is calculated in a similar way. Then the `~label` value, visits, is added to the DataSet and the the properties (which in this case is just the date). 
