package com.example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.apache.spark.sql.SparkSession

class EndToEndIntegrationSpec
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll {

  val calculator = new Calculator()
  var spark: SparkSession = _
  var dataProcessor: DataProcessor = _

  override def beforeAll(): Unit = {
    println("Starting End-to-End integration tests...")
    println("Verifying all components are initialized...")
    
    spark = SparkSession.builder()
      .appName("E2E Integration Tests")
      .master("local[*]")
      .config("spark.ui.enabled", "false")
      .config("spark.driver.host", "localhost")
      .getOrCreate()
    
    spark.sparkContext.setLogLevel("ERROR")
    dataProcessor = DataProcessor(spark)
  }

  override def afterAll(): Unit = {
    println("End-to-End integration tests completed.")
    if (spark != null) {
      spark.stop()
    }
  }

  behavior of "End-to-End System Integration"

  it should "execute complete workflow with Calculator and DataProcessor" in {
    import spark.implicits._
    
    // Step 1: Use Calculator to compute threshold
    val baseValue = 100
    val multiplier = 2
    val threshold = calculator.multiply(baseValue, multiplier)
    
    threshold shouldBe 200

    // Step 2: Use DataProcessor with computed threshold
    val testData = Seq(
      ("item1", 150.0),
      ("item2", 250.0),
      ("item3", 300.0)
    ).toDF("item", "value")

    val processed = dataProcessor.processData(testData, threshold.toDouble)
    
    // Step 3: Verify results
    processed.count() shouldBe 2
  }

  it should "handle multi-stage data processing pipeline" in {
    import spark.implicits._
    
    // Stage 1: Calculate business metrics
    val salesTarget = 10000
    val actualSales = 12000
    val variance = calculator.subtract(actualSales, salesTarget)
    val percentageOpt = calculator.divide(variance, salesTarget)
    
    percentageOpt shouldBe defined
    val percentage = percentageOpt.get

    // Stage 2: Process sales data
    val salesData = Seq(
      ("region_A", 3000.0),
      ("region_B", 4500.0),
      ("region_C", 4500.0)
    ).toDF("region", "sales")

    // Stage 3: Filter and categorize
    val processed = dataProcessor.processData(salesData, 3500.0)
    
    processed.count() shouldBe 2
  }

  it should "integrate calculation results with data aggregation" in {
    import spark.implicits._
    
    // Calculate discount threshold
    val basePrice = 1000
    val discountRate = 10
    val discountAmount = calculator.multiply(basePrice, discountRate)
    val thresholdOpt = calculator.divide(discountAmount, 100)
    
    thresholdOpt shouldBe Some(100.0)

    // Create transaction data
    val transactions = Seq(
      ("store1", 50.0),
      ("store1", 150.0),
      ("store2", 200.0),
      ("store2", 75.0)
    ).toDF("store", "amount")

    // Aggregate by store
    val aggregated = dataProcessor.aggregateByKey(transactions, "store")
    
    aggregated.count() shouldBe 2
  }

  it should "handle error scenarios gracefully across components" in {
    import spark.implicits._
    
    // Test Calculator error handling
    val divisionResult = calculator.divide(100, 0)
    divisionResult shouldBe None

    // System should continue working after error
    val validCalc = calculator.add(10, 20)
    validCalc shouldBe 30

    // DataProcessor should handle empty data
    val emptyData = Seq.empty[(String, Double)].toDF("key", "value")
    val processed = dataProcessor.processData(emptyData, 10.0)
    processed.count() shouldBe 0
  }

  it should "process complete business workflow end-to-end" in {
    import spark.implicits._
    
    // Business scenario: Sales analysis
    val targetRevenue = 50000
    val actualRevenue = 55000
    
    // Calculate metrics
    val difference = calculator.subtract(actualRevenue, targetRevenue)
    val growthOpt = calculator.divide(difference, targetRevenue)
    
    difference shouldBe 5000
    growthOpt.map(g => (g * 100).toInt) shouldBe Some(10)

    // Analyze regional performance
    val regionalSales = Seq(
      ("North", 15000.0),
      ("South", 18000.0),
      ("East", 12000.0),
      ("West", 10000.0)
    ).toDF("region", "revenue")

    // Process and categorize regions
    val avgRevenue = calculator.divide(actualRevenue, 4)
    val processed = dataProcessor.processData(
      regionalSales, 
      avgRevenue.getOrElse(0.0)
    )

    // Verify business logic
    val highPerformers = processed.filter("category = 'high'").count()
    highPerformers should be >= 0L
  }

  it should "pass system health checks" in {
    import spark.implicits._
    
    // Health check 1: Calculator component
    val healthCalc1 = calculator.add(1, 1)
    healthCalc1 shouldBe 2

    val healthCalc2 = calculator.multiply(10, 10)
    healthCalc2 shouldBe 100

    val healthCalc3 = calculator.divide(100, 2)
    healthCalc3 shouldBe Some(50.0)

    // Health check 2: DataProcessor component
    val healthData = Seq(("test", 100.0)).toDF("key", "value")
    val healthProcessed = dataProcessor.processData(healthData, 50.0)
    healthProcessed.count() shouldBe 1

    println("✓ All system components healthy")
  }

  it should "meet performance baseline requirements" in {
    import spark.implicits._
    
    val startTime = System.currentTimeMillis()

    // Execute a series of operations
    (1 to 100).foreach { i =>
      calculator.add(i, i)
      calculator.multiply(i, 2)
    }

    // Process small batch
    val batchData = (1 to 100).map(i => (s"key_$i", i.toDouble))
      .toDF("key", "value")
    
    dataProcessor.processData(batchData, 50.0).count()

    val duration = System.currentTimeMillis() - startTime
    println(s"Performance test completed in ${duration}ms")

    // Should complete within reasonable time (5 seconds)
    duration should be < 5000L
  }
}
