name := "scala-jenkins-demo"
version := "1.0.0"
scalaVersion := "2.13.12"

val sparkVersion = "3.5.0"

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
  "org.apache.spark" %% "spark-sql" % sparkVersion % Provided,
  "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "ch.qos.logback" % "logback-classic" % "1.4.14"
)

dependencyOverrides ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "2.2.0",
  "org.scalariform" %% "scalariform" % "0.2.10",
  "org.scalactic" %% "scalactic" % "3.2.17"
)

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked"
)

Test / testOptions := Seq(
  Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")
)

coverageMinimumStmtTotal := 40
coverageFailOnMinimum := false
coverageHighlighting := true

assembly / assemblyJarName := s"${name.value}-${version.value}-assembly.jar"

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => xs match {
    case "MANIFEST.MF" :: Nil => MergeStrategy.discard
    case "services" :: _ => MergeStrategy.concat
    case _ => MergeStrategy.discard
  }
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}

