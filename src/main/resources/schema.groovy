system.graph('Test_Graph').ifNotExists().create()
:remote config alias g Test_Graph.g

// Property Keys

schema.propertyKey("offer").Text().ifNotExists().create()
schema.propertyKey("category").Text().ifNotExists().create()
schema.propertyKey("quantity").Int().ifNotExists().create()
schema.propertyKey("company").Text().ifNotExists().create()
schema.propertyKey("offervalue").Double().ifNotExists().create()
schema.propertyKey("brand").Text().ifNotExists().create()
schema.propertyKey("chain").Text().ifNotExists().create()
schema.propertyKey("dept").Text().ifNotExists().create()
schema.propertyKey("productsize").Double().ifNotExists().create()
schema.propertyKey("productmeasure").Text().ifNotExists().create()
schema.propertyKey("purchasequantity").Int().ifNotExists().create()
schema.propertyKey("purchaseamount").Double().ifNotExists().create()
schema.propertyKey("customer_id").Text().ifNotExists().create()
schema.propertyKey("date").Timestamp().ifNotExists().create()
schema.propertyKey("market").Int().ifNotExists().create()
schema.propertyKey("repeater").Text().ifNotExists().create()
schema.propertyKey("repeattrips").Int().ifNotExists().create()

// VERTICES

schema.vertexLabel("offer").
      partitionKey("offer").
      //.clusteringKey("category")
      properties(
        "category",
        "quantity",
        "company",
        "offervalue",
        "brand"
).ifNotExists().create()

schema.vertexLabel("product").
       partitionKey("chain", "company", "brand").
       clusteringKey("dept", "category").
       properties(
        "productsize",
        "productmeasure",
        "purchasequantity",
        "purchaseamount",
        "customer_id",
        "date"
).ifNotExists().create()

schema.vertexLabel("store").
       partitionKey("chain").
       properties(
        "market"
).create()

schema.vertexLabel("customer").
       partitionKey("customer_id").
       ifNotExists().create()

// EDGES

schema.edgeLabel("purchases").
       multiple().
       properties(
        "date",
        "purchasequantity",
        "purchaseamount"
        ).
       connection("customer", "product").
       ifNotExists().create()

schema.edgeLabel("offer_used").
       multiple().
       properties(
        "date",
        "repeater",
        "repeattrips"
        ).
       connection("customer", "offer").
       ifNotExists().create()

schema.edgeLabel("visits").
       multiple().
       properties(
        "date"
       ).
       connection("customer", "store").
       ifNotExists().create()
