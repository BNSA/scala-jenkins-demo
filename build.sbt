name := "scala-jenkins-demo"
version := "1.0"
scalaVersion := "2.13.12"

// Configure It (integration test) as a separate test configuration
lazy val It = config("it") extend(Test)
configs(It)
inConfig(It)(Defaults.testSettings)

// CRITICAL: Fork tests in separate JVM with proper Java module access
Test / fork := true
Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat

It / fork := true
It / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat

// Java options for unit tests
Test / javaOptions ++= Seq(
  "-Xmx2g",
  "-XX:+UseG1GC"
)

// Java options for integration tests (Spark requires these)
It / javaOptions ++= Seq(
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.net=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
  "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
  "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
  "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED",
  "-Xmx2g",
  "-XX:+UseG1GC"
)

// Spark and Hadoop dependencies
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.0",
  "org.apache.spark" %% "spark-sql" % "3.5.0",
  "org.apache.hadoop" % "hadoop-client" % "3.3.6",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "ch.qos.logback" % "logback-classic" % "1.4.11",
  
  // Test dependencies (available for both Test and IntegrationTest)
  "org.scalatest" %% "scalatest" % "3.2.17" % "test,it"
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
