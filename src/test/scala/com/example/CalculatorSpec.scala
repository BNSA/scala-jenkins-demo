package com.example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CalculatorSpec extends AnyFlatSpec with Matchers {
  
  val calculator = new Calculator()
  
  "Calculator" should "add two numbers correctly" in {
    calculator.add(2, 3) shouldBe 5
  }
  
  it should "subtract two numbers correctly" in {
    calculator.subtract(5, 3) shouldBe 2
  }
  
  it should "multiply two numbers correctly" in {
    calculator.multiply(4, 3) shouldBe 12
  }
  
  it should "divide two numbers correctly" in {
    calculator.divide(10, 2) shouldBe Some(5.0)
  }
  
  it should "return None when dividing by zero" in {
    calculator.divide(10, 0) shouldBe None
  }
}
