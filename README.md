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

### How to Run 

***Build***

Build this project from the parent directory:
```sh
$ mvn clean package
```
This project was designed for use with DSE 6.0. If DSE 6 jars are not in the public repo yet, you will have to follow the instructions in the `dse-eap6-dep.txt` located in the resources directory. 

***Load Data***

Get the data from here: https://www.kaggle.com/c/acquire-valued-shoppers-challenge/data and download the following files: transactions, offers, trainHistory and load them into DSEFS

Ex: 
```sh
$ dse fs
$ mkdir data
$ put /home/chucknorris/transactions.csv /data/
```

***Run Spark Job***

Submit the spark job with: 

```sh
$ dse spark-submit --class com.spark.graphframes.App dseGraphFrames-1.0-SNAPSHOT.jar
```

I used the following spark submit parameters on m4.4xlarge machines: 
```sh
$ dse spark-submit --class com.spark.graphframes.App dseGraphFrames-1.0-SNAPSHOT.jar --executor-memory=22G
```

### Graph Model 

***Create Schema***

Run `schema.grooxy` in the resources directory to create the graph schema. This can be run in DataStax Studio or in the Gremlin console. 

***Schema Description***

Here is a diagram showing the schema:


<p align="center">
    <img src="https://image.ibb.co/gbU1En/schema_view.png" alt="image" width="40%">
</p>

Bold: Partition Key

Italic: Clustering Column


***Vertices:***

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


***Edges:***

|visits|offer_used|purchases|
|--------------|--------------|--------------|
|date|date|date|
| |repeater|purchasequantity|
| |repeattrips|purchaseamount|


### Vertices DataSet

Create a dataset with the desired columns and then create a column name ~label with a value of the respective label. 

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

Here we created a DataSet for the **offer vertex** with the label ~offer. Notice how this matches what is defined in your schema: 

<p align="center">
    <img src="https://image.ibb.co/cjEq77/label_description.png" alt="image" width="40%">
</p>

### Edges DataSet

To create an Edge DataSet for Customer --> Store, we must add the following columns: srcLabel, dstLabel, edgeLabel. 

srcLabel: the label of the originating vertex (in this schema it is customer)
dstLabel: the label of the destination vertex (in this schema it is store)
edgeLabel: the label of the edge (in this schema it is visits)

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
Let's run through this example for customer 86246. In the spark repl, we type `custToStore.select("src").limit(1)`. The result is some seeminly arbitary value like: *customer:AAAACTEwMzk4NTI0Ng==*. This is a Spark/DSE Graph Frames id that is created based off of the customer_e PK. The dst value is calculated in a similar way. Then the ~label value, visits, is added to the DataSet and the the properties (which in this case is just the date). 
