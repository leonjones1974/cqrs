name := "cqrs"
version := "1.0-SNAPSHOT"
scalaVersion := "2.11.8"


libraryDependencies ++= Seq (
 // -- testing --
  "org.scalatest" %% "scalatest" % "2.2.2"
)

(testOptions in Test) += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/scalatest-report")

