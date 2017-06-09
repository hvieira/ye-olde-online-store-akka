name := """ye-olde-online-store-akka"""

version := "1.0"

scalaVersion := "2.12.2"

lazy val akkaVersion = "2.5.2"
lazy val akkaHttpVersion = "10.0.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
//  "io.spray" %%  "spray-json" % "1.3.3",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.7",
  "com.pauldijou" %% "jwt-core" % "0.12.1",

  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)


fork in run := true