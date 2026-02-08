package com.example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.apache.spark.sql.SparkSession

class DataProcessorIntegrationSpec
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll {

  var spark: SparkSession = _
  var processor: DataProcessor = _

  override def beforeAll(): Unit = {
    println("Initializing Spark session for integration tests...")
    spark = SparkSession.builder()
      .appName("DataProcessor Integration Tests")
      .master("local[*]")
      .config("spark.ui.enabled", "false")
      .config("spark.driver.host", "localhost")
      .getOrCreate()
    
    spark.sparkContext.setLogLevel("ERROR")
    processor = DataProcessor(spark)
  }

  override def afterAll(): Unit = {
    println("Cleaning up Spark session...")
    if (spark != null) {
      spark.stop()
    }
  }

  behavior of "DataProcessor Integration Tests"

  it should "process data with threshold filtering and transformation" in {
    import spark.implicits._
    
    val testData = Seq(
      ("A", 10.0),
      ("B", 20.0),
      ("C", 30.0),
      ("D", 5.0)
    ).toDF("key", "value")

    val result = processor.processData(testData, 15.0)
    val collected = result.collect()

    collected.length shouldBe 2
    collected.map(_.getAs[String]("key")) should contain allOf ("B", "C")
    collected.find(_.getAs[String]("key") == "B").get.getAs[Double]("processed") shouldBe 40.0
    collected.find(_.getAs[String]("key") == "C").get.getAs[Double]("processed") shouldBe 60.0
  }

  it should "correctly categorize values based on threshold" in {
    import spark.implicits._
    
    val testData = Seq(
      ("A", 20.0),
      ("B", 35.0),
      ("C", 50.0)
    ).toDF("key", "value")

    val result = processor.processData(testData, 10.0)
    val collected = result.collect()

    val categories = collected.map(r => 
      (r.getAs[String]("key"), r.getAs[String]("category"))
    ).toMap

    categories("A") shouldBe "medium"
    categories("B") shouldBe "high"
    categories("C") shouldBe "high"
  }

  it should "aggregate data by key column correctly" in {
    import spark.implicits._
    
    val testData = Seq(
      ("group1", 10.0),
      ("group1", 20.0),
      ("group2", 15.0),
      ("group2", 25.0),
      ("group1", 30.0)
    ).toDF("category", "value")

    val result = processor.aggregateByKey(testData, "category")
    val collected = result.collect()

    collected.length shouldBe 2
    
    val group1 = collected.find(_.getAs[String]("category") == "group1").get
    group1.getAs[Long]("count") shouldBe 3
    group1.getAs[Double]("avg_value") shouldBe 20.0
    group1.getAs[Double]("max_value") shouldBe 30.0
    group1.getAs[Double]("min_value") shouldBe 10.0

    val group2 = collected.find(_.getAs[String]("category") == "group2").get
    group2.getAs[Long]("count") shouldBe 2
    group2.getAs[Double]("avg_value") shouldBe 20.0
  }

  it should "handle empty dataframes gracefully" in {
    import spark.implicits._
    
    val emptyData = Seq.empty[(String, Double)].toDF("key", "value")

    val result = processor.processData(emptyData, 10.0)
    result.count() shouldBe 0
  }

  it should "process large datasets efficiently" in {
    import spark.implicits._
    
    val largeData = (1 to 1000).map(i => (s"key_$i", i.toDouble))
      .toDF("key", "value")

    val result = processor.processData(largeData, 500.0)
    val count = result.count()

    count shouldBe 500
    result.filter("category = 'high'").count() should be > 0L
  }

  it should "execute complete ETL pipeline workflow" in {
    import spark.implicits._
    
    // Stage 1: Raw data ingestion
    val rawData = Seq(
      ("product_A", 100.0),
      ("product_B", 150.0),
      ("product_C", 200.0),
      ("product_D", 50.0),
      ("product_E", 175.0)
    ).toDF("product", "value")

    // Stage 2: Process and filter
    val processed = processor.processData(rawData, 75.0)

    // Stage 3: Aggregate
    val aggregated = processor.aggregateByKey(
      processed.withColumn("group", 
        org.apache.spark.sql.functions.substring(
          org.apache.spark.sql.functions.col("product"), 9, 1
        )
      ),
      "group"
    )

    // Verify pipeline
    processed.count() shouldBe 4
    aggregated.count() should be > 0L
  }
}
