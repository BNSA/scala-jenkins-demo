name := "scala-jenkins-demo"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.13.12"

// Use sbt's built-in IntegrationTest configuration
configs(IntegrationTest)
Defaults.itSettings

IntegrationTest / scalaSource := baseDirectory.value / "src" / "it" / "scala"
IntegrationTest / resourceDirectory := baseDirectory.value / "src" / "it" / "resources"

// Dependencies
libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.10.0",
  "org.scalatest" %% "scalatest" % "3.2.15" % "test,it",
  "org.scalatestplus" %% "mockito-4-6" % "3.2.15.0" % "test,it",
  
  // Flexmark dependencies for HTML report generation
  "com.vladsch.flexmark" % "flexmark-all" % "0.64.8" % "test,it",
  "com.vladsch.flexmark" % "flexmark-util" % "0.64.8" % "test,it",
  "com.vladsch.flexmark" % "flexmark-util-ast" % "0.64.8" % "test,it"
)

// Compiler options
scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint"
)

// Test options - this generates HTML reports
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports/unit")
IntegrationTest / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports/integration")

// Coverage settings
coverageEnabled := true
coverageMinimumStmtTotal := 80
coverageFailOnMinimum := false
coverageHighlighting := true

// Assembly settings
assembly / assemblyJarName := s"${name.value}-${version.value}.jar"
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
