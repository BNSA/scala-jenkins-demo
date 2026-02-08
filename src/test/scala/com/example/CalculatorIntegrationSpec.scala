package com.example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll

class CalculatorIntegrationSpec
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll {

  val calculator = new Calculator()

  override def beforeAll(): Unit = {
    println("Setting up Calculator integration test environment...")
  }

  override def afterAll(): Unit = {
    println("Tearing down Calculator integration test environment...")
  }

  behavior of "Calculator Integration Tests"

  it should "handle complex multi-step calculation workflows" in {
    val step1 = calculator.add(100, 50)
    val step2 = calculator.subtract(step1, 25)
    val step3 = calculator.multiply(step2, 2)
    val step4 = calculator.divide(step3, 5)

    step4 shouldBe Some(50.0)
  }

  it should "perform end-to-end financial calculation scenario" in {
    val baseAmount = 1000
    val taxRate = 18
    val discount = 50

    val tax = calculator.multiply(baseAmount, taxRate)
    val totalWithTax = calculator.add(baseAmount, tax)
    val finalAmount = calculator.subtract(totalWithTax, discount)

    finalAmount shouldBe 18950
  }

  it should "validate calculation pipeline with boundary values" in {
    val largeNumber = 999999
    val result1 = calculator.add(largeNumber, 1)
    result1 shouldBe 1000000

    val result2 = calculator.divide(result1, 1000)
    result2 shouldBe Some(1000.0)

    val result3Opt = result2.map(_.toInt)
    val result3 = result3Opt.flatMap(calculator.divide(_, 10))
    result3 shouldBe Some(100.0)
  }

  it should "handle negative numbers throughout calculation workflow" in {
    val debit = -500
    val credit = 1000

    val balance1 = calculator.add(debit, credit)
    balance1 shouldBe 500

    val balance2 = calculator.subtract(balance1, 200)
    balance2 shouldBe 300

    val multiplied = calculator.multiply(balance2, 2)
    multiplied shouldBe 600
  }

  it should "properly handle zero in calculation chains" in {
    val result1 = calculator.multiply(100, 0)
    result1 shouldBe 0

    val result2 = calculator.add(result1, 50)
    result2 shouldBe 50

    val result3 = calculator.subtract(result2, 0)
    result3 shouldBe 50
  }

  it should "handle division by zero in workflow" in {
    val result = calculator.divide(100, 0)
    result shouldBe None
  }

  it should "maintain precision in financial calculation workflows" in {
    val principal = 10000
    val rate = 5
    val time = 3

    val interest = calculator.multiply(principal, rate)
    val totalInterest = calculator.divide(interest, 100)
    
    totalInterest shouldBe Some(500.0)
    
    val yearlyInterest = totalInterest.map(_.toInt)
    val totalForYears = yearlyInterest.map(calculator.multiply(_, time))

    totalForYears shouldBe Some(1500)
  }

  it should "process batch calculations correctly" in {
    val numbers = List(10, 20, 30, 40, 50)
    
    val sum = numbers.reduce((a, b) => calculator.add(a, b))
    sum shouldBe 150

    val product = numbers.take(3).reduce((a, b) => calculator.multiply(a, b))
    product shouldBe 6000
  }

  it should "handle Option chaining in division workflows" in {
    val result1 = calculator.divide(100, 2)
    val result2 = result1.flatMap(r => calculator.divide(r.toInt, 5))
    val result3 = result2.flatMap(r => calculator.divide(r.toInt, 2))

    result3 shouldBe Some(5.0)
  }

  it should "validate None propagation in calculation chains" in {
    val result1 = calculator.divide(100, 0)
    val result2 = result1.map(r => calculator.add(r.toInt, 50))

    result1 shouldBe None
    result2 shouldBe None
  }
}
