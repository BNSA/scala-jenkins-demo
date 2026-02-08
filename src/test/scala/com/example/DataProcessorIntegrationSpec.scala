package com.example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.apache.spark.sql.SparkSession

class DataProcessorIntegrationSpec
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll {

  @transient private var sparkSession: SparkSession = _
  private var processor: DataProcessor = _

  override def beforeAll(): Unit = {
    sparkSession = SparkSession
      .builder()
      .appName("DataProcessor Integration Tests")
      .master("local[2]")
      .config("spark.ui.enabled", "false")
      .config("spark.sql.shuffle.partitions", "2")
      .getOrCreate()

    sparkSession.sparkContext.setLogLevel("ERROR")
    processor = DataProcessor(sparkSession)
    
    println("✓ Spark session initialized for DataProcessor integration tests")
  }

  override def afterAll(): Unit = {
    if (sparkSession != null) {
      sparkSession.stop()
      println("✓ Spark session stopped")
    }
  }

  behavior of "DataProcessor Integration Tests"

  it should "process data with threshold filtering end-to-end" in {
    import sparkSession.implicits._
    
    val inputData = Seq(
      ("product1", 100.0),
      ("product2", 50.0),
      ("product3", 200.0),
      ("product4", 75.0)
    ).toDF("name", "value")

    val result = processor.processData(inputData, threshold = 60.0)
    
    result.count() shouldBe 3
    result.filter($"category" === "high").count() shouldBe 1
    result.filter($"category" === "medium").count() shouldBe 2
  }

  it should "aggregate data by key correctly" in {
    import sparkSession.implicits._
    
    val inputData = Seq(
      ("A", 100.0),
      ("B", 50.0),
      ("A", 200.0),
      ("B", 75.0),
      ("A", 150.0)
    ).toDF("key", "value")

    val result = processor.aggregateByKey(inputData, "key")
    
    result.count() shouldBe 2
    
    val keyA = result.filter($"key" === "A").collect()(0)
    keyA.getAs[Long]("count") shouldBe 3
    keyA.getAs[Double]("avg_value") shouldBe 150.0
    keyA.getAs[Double]("max_value") shouldBe 200.0
    keyA.getAs[Double]("min_value") shouldBe 100.0
  }

  it should "handle empty dataset gracefully" in {
    import sparkSession.implicits._
    
    val emptyData = Seq.empty[(String, Double)].toDF("key", "value")
    
    val result = processor.processData(emptyData, threshold = 50.0)
    result.count() shouldBe 0
  }

  it should "perform complex ETL workflow" in {
    import sparkSession.implicits._
    
    val rawData = Seq(
      ("region1", 120.0),
      ("region2", 80.0),
      ("region1", 150.0),
      ("region3", 200.0),
      ("region2", 90.0)
    ).toDF("region", "value")

    val processed = processor.processData(rawData, threshold = 85.0)
    val aggregated = processor.aggregateByKey(processed, "region")
    
    aggregated.count() shouldBe 3
    aggregated.columns should contain allOf ("region", "count", "avg_value")
  }

  it should "categorize values correctly based on threshold" in {
    import sparkSession.implicits._
    
    val testData = Seq(
      ("item1", 100.0),
      ("item2", 150.0),
      ("item3", 250.0)
    ).toDF("name", "value")

    val result = processor.processData(testData, threshold = 100.0)
    
    result.filter($"category" === "medium").count() shouldBe 1
    result.filter($"category" === "high").count() shouldBe 2
  }

  it should "handle large threshold filtering" in {
    import sparkSession.implicits._
    
    val testData = Seq(
      ("A", 50.0),
      ("B", 100.0),
      ("C", 150.0)
    ).toDF("key", "value")

    val result = processor.processData(testData, threshold = 1000.0)
    
    result.count() shouldBe 0
  }
}
