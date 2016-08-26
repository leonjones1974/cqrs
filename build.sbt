name := "cqrs"
version := "1.0-SNAPSHOT"
scalaVersion := "2.11.8"


libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.2.4"
libraryDependencies += "com.github.nscala-money" %% "nscala-money" % "0.11.0"
libraryDependencies += "com.github.nscala-time" % "nscala-time_2.11" % "2.12.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.2" % "test"
libraryDependencies += "org.pegdown" % "pegdown" % "1.0.2" % "test"

(testOptions in Test) += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/scalatest-report")

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

credentials += Credentials(Path.userHome / ".sonatype" / ".credentials")

publishMavenStyle := true
publishArtifact in Test := true

pomExtra := (
  <url>http://camsw.uk/cqrs</url>
    <licenses>
      <license>
        <name>BSD-style</name>
        <url>http://www.opensource.org/licenses/bsd-license.php</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:leonjones1974/cqrs.git</url>
      <connection>scm:git:git@github.com:leonjones1974/cqrs.git</connection>
    </scm>
    <developers>
      <developer>
        <id>leonjones1974</id>
        <name>Leon Jones</name>
        <url>http://camsw.uk</url>
      </developer>
    </developers>)