package com.spark.graphframes

import com.datastax.bdp.graph.spark.graphframe._
import org.apache.spark.ml.recommendation.ALS
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.ml.evaluation.RegressionEvaluator


object App {
  def main(args: Array[String]):Unit = {

    // Change this to the name of your graph
    val graphName = "Test_Graph"

    val spark = SparkSession
      .builder
      .appName("Graph Load Application")
      .enableHiveSupport()
      .getOrCreate()

    val g = spark.dseGraph(graphName)


    // Create Schemas for DataSets
    def transactionsSchema():StructType = {
      StructType(Array(
        StructField("id", StringType, true),
        StructField("chain",StringType,true),
        StructField("dept",StringType,true),
        StructField("category",StringType,true),
        StructField("company",StringType,true),
        StructField("brand",StringType,true),
        StructField("date",TimestampType,true),
        StructField("productsize",DoubleType,true),
        StructField("productmeasure",StringType,true),
        StructField("purchasequantity",IntegerType,true),
        StructField("purchaseamount",DoubleType,true)))
    }

    def offersSchema():StructType = {
      StructType(Array(
        StructField("offer",StringType,true),
        StructField("category",StringType,true),
        StructField("quantity",IntegerType,true),
        StructField("company",StringType,true),
        StructField("offervalue",DoubleType,true),
        StructField("brand",StringType,true)))
    }

    def historySchema():StructType = {
      StructType(Array(
        StructField("id",StringType,true),
        StructField("chain",StringType,true),
        StructField("offer",StringType,true),
        StructField("market",IntegerType,true),
        StructField("repeattrips",IntegerType,true),
        StructField("repeater",StringType,true),
        StructField("date",TimestampType,true)
      ))
    }

    // Read CSV Files
    // .option("inferSchema", "true") --> Use this if you do not want to define schema explicitly

    val transactions = spark.sqlContext.read.format("csv").option("header", "true").schema(transactionsSchema).load("dsefs:///data/transactions.csv")
    val offer = spark.sqlContext.read.format("csv").option("header", "true").schema(offersSchema).load("dsefs:///data/offers.csv")
    val history = spark.sqlContext.read.format("csv").option("header", "true").schema(historySchema).load("dsefs:///data/trainHistory.csv")


    // WRITE OUT VERTICES

    // Customer Vertex: Take distinct transactions id and persist
    val tx = transactions.select(transactions("id")).distinct

    val customers = tx.select(col("id") as "customer_id").withColumn("~label", lit("customer"))

    println("\nWriting Customer Vertices")
    g.updateVertices(customers)

    // Offer Vertex: Use the offers DataSet
    val offers = offer.select(
      col("offer") as "offer",
      col("category"),
      col("quantity"),
      col("company"),
      col("offervalue"),
      col("brand")
    ).withColumn("~label", lit("offer"))

    println("\nWriting Offer Vertices")
    g.updateVertices(offers)

    // Product Vertex: Use part of the transactions DataSet
    val products = transactions.select(
      col("chain") as "chain",
      col("dept") as "dept",
      col("category") as "category",
      col("company") as "company",
      col("brand") as "brand",
      col("productsize") as "productsize",
      col("productmeasure") as "productmeasure",
      col("purchasequantity"),
      col("purchaseamount"),
      col("id") as "customer_id",
      col("date")
     ).withColumn("~label", lit("product"))

    println("\nWriting Product Vertices")
    g.updateVertices(products)

    // Store Vertex: Select distinct chains from transactions and join to history where you select chain, market
    val tempA = transactions.select(transactions("chain")).distinct
    val tempB = history.select(history("chain"),history("market"))
    val store = tempA.join(tempB, "chain")

    val stores = store.select(
      col("chain") as "chain",
      col("market")
    ).withColumn("~label", lit("store"))

    println("\nWriting Store Vertices")
    g.updateVertices(stores)

    // WRITE OUT EDGES

    // Add some columns to the transactions DataSet
    val txEdge = transactions.withColumn("srcLabel", lit("customer")).withColumnRenamed("id", "customer_id").withColumn("dstLabel", lit("store")).withColumn("edgeLabel", lit("visits"))
    val custToStore = txEdge.select(g.idColumn(col("srcLabel"), col("customer_id")) as "src", g.idColumn(col("dstLabel"), col("chain")) as "dst", col("edgeLabel") as "~label", col("date") as "date")
    println("\nWriting Visit Vertices")
    g.updateEdges(custToStore)

    //val txHistory = history.join(offer,"offer")
    val offerEdge = history.withColumn("srcLabel", lit("customer")).withColumnRenamed("id", "customer_id").withColumn("dstLabel", lit("offer")).withColumn("edgeLabel", lit("offer_used"))
    val custToOffer = offerEdge.select(g.idColumn(col("srcLabel"), col("customer_id")) as "src", g.idColumn(col("dstLabel"), col("offer")) as "dst", col("edgeLabel") as "~label", col("date") as "date", col("repeattrips") as "repeattrips", col("repeater") as "repeater")
    println("\nWriting Offer Used Vertices")
    g.updateEdges(custToOffer)

    val purchaseEdge = transactions.withColumn("srcLabel", lit("customer")).withColumnRenamed("id", "customer_id").withColumn("dstLabel", lit("product")).withColumn("edgeLabel", lit("purchases"))
    val custToProduct = purchaseEdge.select(g.idColumn(col("srcLabel"), col("customer_id")) as "src", g.idColumn(col("dstLabel"), col("brand"), col("category"), col("chain"), col("company"), col("dept")) as "dst", col("edgeLabel") as "~label", col("date") as "date", col("purchasequantity") as "purchasequantity", col("purchaseamount") as "purchaseamount")
    println("\nWriting Purchases Vertices")
    g.updateEdges(custToProduct)

    // COLLABORATIVE FILTERING EXAMPLE 
    // Can comment out this out if only loading data
    transactions.createOrReplaceTempView("makeFeatures")
    val prodList = spark.sql("SELECT row_number() over (order by chain, company, brand, dept, category) as pid, chain, company, brand, dept, category FROM (SELECT DISTINCT chain, company, brand, dept, category FROM makeFeatures) derived")
    transactions.createOrReplaceTempView("prodList")
    val features = transactions.join(prodList, Seq("chain", "company", "brand", "dept", "category")).select(col("id"),col("pid"),col("purchasequantity"))
    val finalFeatures = features.withColumn("id",col("id").cast(IntegerType)).withColumn("purchasequantity",col("purchasequantity").cast(IntegerType)).withColumn("pid",col("pid").cast(IntegerType))
    // Do Collaborative Filtering
    val Array(training, test) = finalFeatures.randomSplit(Array(0.8, 0.2))
    val als = new ALS().setMaxIter(5).setRegParam(0.01).setUserCol("id").setItemCol("pid").setRatingCol("purchasequantity")
    val model = als.fit(training)
    model.setColdStartStrategy("drop")
    val predictions = model.transform(test)
    val evaluator = new RegressionEvaluator().setMetricName("rmse").setLabelCol("purchasequantity").setPredictionCol("prediction")
    val rmse = evaluator.evaluate(predictions)
    println(s"Root-mean-square error = $rmse")
    // Create 5 recommendations
    val prodRecsForUser = model.recommendForAllUsers(5)

  }
}
