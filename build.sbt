name := """ye-olde-online-store-akka"""

version := "1.0"

scalaVersion := "2.11.6"

lazy val akkaVersion = "2.4.18"
lazy val akkaHttpVersion = "10.0.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.pauldijou" %% "jwt-core" % "0.12.1",
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)


fork in run := true