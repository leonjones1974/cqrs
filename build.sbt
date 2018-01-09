name := "cqrs"
version := "1.1.1-SNAPSHOT"
scalaVersion := "2.12.4"
organization := "uk.camsw"

crossScalaVersions := Seq("2.11.11", "2.12.4")


libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.2.18"
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.18.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4"
libraryDependencies += "org.pegdown" % "pegdown" % "1.0.2" % "test"
libraryDependencies += "com.google.guava" % "guava" % "23.0"
libraryDependencies += "org.slf4j" % "jul-to-slf4j" % "1.7.6"
libraryDependencies += "org.clapper" %% "grizzled-slf4j" % "1.3.0"
libraryDependencies +=  "ch.qos.logback" % "logback-classic" % "1.1.7" % "test"
libraryDependencies +=  "com.github.pathikrit" %% "better-files" % "3.4.0"
libraryDependencies +=  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % "test"


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