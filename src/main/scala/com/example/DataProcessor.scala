package com.example

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import com.typesafe.scalalogging.LazyLogging

class DataProcessor(spark: SparkSession) extends LazyLogging {
  
  import spark.implicits._
  
  /**
   * Process data by filtering and transforming
   */
  def processData(df: DataFrame, threshold: Double): DataFrame = {
    logger.info(s"Processing data with threshold: $threshold")
    
    df.filter($"value" > threshold)
      .withColumn("processed", $"value" * 2)
      .withColumn("category", 
        when($"value" > threshold * 2, "high")
          .when($"value" > threshold, "medium")
          .otherwise("low")
      )
  }
  
  /**
   * Aggregate data by key
   */
  def aggregateByKey(df: DataFrame, keyColumn: String): DataFrame = {
    logger.info(s"Aggregating by column: $keyColumn")
    
    df.groupBy(keyColumn)
      .agg(
        count("*").as("count"),
        avg("value").as("avg_value"),
        max("value").as("max_value"),
        min("value").as("min_value")
      )
  }
}

object DataProcessor {
  def apply(spark: SparkSession): DataProcessor = new DataProcessor(spark)
}
