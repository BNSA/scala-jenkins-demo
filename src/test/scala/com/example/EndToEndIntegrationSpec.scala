package com.example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.apache.spark.sql.SparkSession

class EndToEndIntegrationSpec
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll {

  private val calculator = new Calculator()
  @transient private var sparkSession: SparkSession = _
  private var processor: DataProcessor = _

  override def beforeAll(): Unit = {
    sparkSession = SparkSession
      .builder()
      .appName("End-to-End Integration Tests")
      .master("local[2]")
      .config("spark.ui.enabled", "false")
      .config("spark.sql.shuffle.partitions", "2")
      .getOrCreate()

    sparkSession.sparkContext.setLogLevel("ERROR")
    processor = DataProcessor(sparkSession)
    
    println("✓ Complete integration test environment initialized")
  }

  override def afterAll(): Unit = {
    if (sparkSession != null) {
      sparkSession.stop()
      println("✓ Complete integration test environment cleaned up")
    }
  }

  behavior of "End-to-End Integration Tests"

  it should "integrate Calculator and DataProcessor for business workflow" in {
    import sparkSession.implicits._
    
    val baseValue = 100
    val multiplier = 3
    val threshold = calculator.multiply(baseValue, multiplier)

    val salesData = Seq(
      ("product1", 250.0),
      ("product2", 150.0),
      ("product3", 400.0),
      ("product4", 200.0)
    ).toDF("item", "value")

    val filtered = processor.processData(salesData, threshold = threshold.toDouble)
    filtered.count() shouldBe 2
  }

  it should "perform complete data transformation pipeline" in {
    import sparkSession.implicits._
    
    val rawRevenue = Seq(
      ("North", 100.0),
      ("South", 200.0),
      ("North", 150.0),
      ("East", 300.0)
    ).toDF("region", "sales")

    val processed = processor.processData(rawRevenue, threshold = 50.0)
    val aggregated = processor.aggregateByKey(processed, "region")
    
    aggregated.count() should be > 0L
    aggregated.columns should contain("count")
  }

  it should "handle calculator results in data processing workflows" in {
    import sparkSession.implicits._
    
    val discount = 20
    val basePrice = 100
    val finalPrice = calculator.subtract(basePrice, discount)

    val priceData = Seq(
      ("item1", 90.0),
      ("item2", 70.0),
      ("item3", 100.0)
    ).toDF("store", "amount")

    val result = processor.processData(priceData, threshold = finalPrice.toDouble)
    result.count() shouldBe 1
  }

  it should "validate end-to-end system health check" in {
    import sparkSession.implicits._
    
    val calculatorTest = calculator.add(50, 50)
    calculatorTest shouldBe 100

    val emptyData = Seq.empty[(String, Double)].toDF("key", "value")
    val processedEmpty = processor.processData(emptyData, threshold = 0.0)
    processedEmpty.count() shouldBe 0
  }

  it should "perform complex aggregation with calculated thresholds" in {
    import sparkSession.implicits._
    
    val healthData = Seq(("test", 100.0)).toDF("key", "value")
    val aggregated = processor.aggregateByKey(healthData, "key")
    
    aggregated.count() shouldBe 1
    aggregated.columns should contain allOf ("key", "count", "avg_value")
  }

  it should "handle large-scale data processing with calculations" in {
    import sparkSession.implicits._
    
    val batchSize = 100
    val multiplier = 2
    val expectedSize = calculator.multiply(batchSize, multiplier)

    val largeDataset = (1 to expectedSize)
      .map(i => (s"item_$i", i.toDouble))
      .toDF("key", "value")

    val processed = processor.processData(largeDataset, threshold = 50.0)
    processed.count() should be > 100L
  }

  it should "integrate all components for financial reporting workflow" in {
    import sparkSession.implicits._
    
    val taxRate = 18
    val baseAmount = 1000
    val taxAmount = calculator.multiply(baseAmount, taxRate)
    
    val revenueData = Seq(
      ("Q1", 15000.0),
      ("Q2", 18000.0),
      ("Q3", 20000.0),
      ("Q4", 22000.0)
    ).toDF("region", "revenue")

    val filtered = processor.processData(revenueData, threshold = taxAmount.toDouble)
    val summary = processor.aggregateByKey(filtered, "region")
    
    summary.count() shouldBe 4
    
    val totalAvg = summary.select($"avg_value").collect().map(_.getDouble(0)).sum / summary.count()
    totalAvg should be > 0.0
  }
}
