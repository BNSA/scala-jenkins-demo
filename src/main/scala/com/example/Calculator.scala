package com.example

class Calculator {
  def add(a: Int, b: Int): Int = a + b
  
  def subtract(a: Int, b: Int): Int = a - b
  
  def multiply(a: Int, b: Int): Int = a * b
  
  def divide(a: Int, b: Int): Option[Double] = {
    if (b == 0) None else Some(a.toDouble / b.toDouble)
  }
}
