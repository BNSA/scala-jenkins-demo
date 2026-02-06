name := "scala-jenkins-demo"

version := "1.0.0"

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

// Test configuration
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
