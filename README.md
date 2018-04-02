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
$ put /home/checknorris/transactions.csv /data/
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

![](https://image.ibb.co/gbU1En/schema_view.png | width=50%)


Bold: Partition Key
Italic: Clustering Column

***Vertices:***
|Product|Customer|Store|Offer|
|--------------|--------------|--------------|--------------|
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

![alt text](https://ibb.co/fT0mfS)

### Edges DataSet

### MlLib Collaborative Filtering Demo 
