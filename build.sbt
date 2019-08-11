name := "EShop"

version := "0.2"

scalaVersion := "2.13.0"

libraryDependencies += "org.typelevel" %% "cats-core" % "2.0.0-M4"

val akkaVersion = "2.5.23"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test")


// scalaFmt
scalafmtOnCompile := true