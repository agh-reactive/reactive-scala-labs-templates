enablePlugins(GatlingPlugin)

name := "EShop"

version := "0.2"

scalaVersion := "2.12.10"

val akkaVersion = "2.5.26"
val akkaHttpVersion = "10.1.9"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka"         %% "akka-cluster"              % akkaVersion,
  "com.typesafe.akka"         %% "akka-cluster-tools"        % akkaVersion,
  "org.iq80.leveldb"            % "leveldb"          % "0.9",
  "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
  "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.15.2",
  "com.typesafe.akka"         %% "akka-http"                 % akkaHttpVersion,
  "com.typesafe.akka"         %% "akka-http-spray-json"      % akkaHttpVersion,
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "io.gatling"                % "gatling-http"               % "3.2.1",
)

libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.2.1" % "test,it"
libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "3.2.1" % "test,it"

// scalaFmt
scalafmtOnCompile := true