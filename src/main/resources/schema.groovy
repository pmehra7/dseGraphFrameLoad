system.graph('Test_Graph').create()
:remote config alias g Test_Graph.g

// Property Keys

schema.propertyKey("offer").Text().create()
schema.propertyKey("category").Text().create()
schema.propertyKey("quantity").Int().create()
schema.propertyKey("company").Text().create()
schema.propertyKey("offervalue").Double().create()
schema.propertyKey("brand").Text().create()
schema.propertyKey("chain").Text().create()
schema.propertyKey("dept").Text().create()
schema.propertyKey("productsize").Double().create()
schema.propertyKey("productmeasure").Text().create()
schema.propertyKey("purchasequantity").Int().create()
schema.propertyKey("purchaseamount").Double().create()
schema.propertyKey("customer_id").Text().create()
schema.propertyKey("date").Timestamp().create()
schema.propertyKey("market").Int().create()
schema.propertyKey("repeater").Text().create()
schema.propertyKey("repeattrips").Int().create()

// VERTICES

schema
        .vertexLabel("offer")
        .partitionKey("offer")
//.clusteringKey("category")
        .properties(
        "category",
        "quantity",
        "company",
        "offervalue",
        "brand"
).create()

schema
        .vertexLabel("product")
        .partitionKey("chain", "company", "brand")
        .clusteringKey("dept", "category")
        .properties(
        "productsize",
        "productmeasure",
        "purchasequantity",
        "purchaseamount",
        "customer_id",
        "date"
).create()

schema
        .vertexLabel("store")
        .partitionKey("chain")
        .properties(
        "market"
).create()

schema
        .vertexLabel("customer")
        .partitionKey("customer_id")
        .create()

// EDGES

schema.edgeLabel("purchases")
        .multiple()
        .properties(
        "date",
        "purchasequantity",
        "purchaseamount"
)
        .connection("customer", "product")
        .create()

schema.edgeLabel("offer_used")
        .multiple()
        .properties(
        "date",
        "repeater",
        "repeattrips"
)
        .connection("customer", "offer")
        .create()

schema.edgeLabel("visits")
        .multiple()
        .properties(
        "date"
)
        .connection("customer", "store")
        .create()
