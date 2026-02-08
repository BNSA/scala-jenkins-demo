name := "scala-jenkins-demo"
version := "1.0"
scalaVersion := "2.13.12"

// Classloader fix for Spark integration tests
Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat

// Add Java options for Spark to access internal JDK modules
Test / javaOptions ++= Seq(
  "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED"
)

// Spark and Hadoop dependencies
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.0",
  "org.apache.spark" %% "spark-sql" % "3.5.0",
  "org.apache.hadoop" % "hadoop-client" % "3.3.6",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "ch.qos.logback" % "logback-classic" % "1.4.11",
  
  // Test dependencies
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

// Assembly settings for creating fat JAR
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("reference.conf")    => MergeStrategy.concat
  case x if x.endsWith(".proto")     => MergeStrategy.rename
  case _                             => MergeStrategy.first
}

assembly / assemblyJarName := s"${name.value}-${version.value}.jar"

// Scoverage settings
coverageMinimumStmtTotal := 80
coverageFailOnMinimum := false
coverageHighlighting := true

// Scalafmt
scalafmtOnCompile := true
