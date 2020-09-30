name := "EShop"

version := "0.2"

scalaVersion := "2.13.3"

val akkaVersion = "2.6.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"               % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit"             % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % "test",
  "org.scalatest"     %% "scalatest"                % "3.2.2" % "test",
  "ch.qos.logback"    % "logback-classic"           % "1.2.3"
)

// scalaFmt
scalafmtOnCompile := true
