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

Run `schema.grooxy` in the resources directory to create the graph schema.

### Vertices DataSet

### Edges DataSet

### MlLib Collaborative Filtering Demo 
