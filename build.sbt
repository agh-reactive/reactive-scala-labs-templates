name := "EShop"

version := "0.2"

scalaVersion := "2.13.3"

libraryDependencies += "org.typelevel" %% "cats-core" % "2.0.0"

val akkaVersion = "2.6.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-actor"                % akkaVersion,
  "com.typesafe.akka"         %% "akka-testkit"              % akkaVersion % "test",
  "com.typesafe.akka"         %% "akka-actor-typed"          % akkaVersion,
  "com.typesafe.akka"         %% "akka-actor-testkit-typed"  % akkaVersion % "test",
  "com.typesafe.akka"         %% "akka-persistence"          % akkaVersion,
  "com.typesafe.akka"         %% "akka-persistence-query"    % akkaVersion,
  "com.typesafe.akka"         %% "akka-persistence-typed"    % akkaVersion,
  "com.typesafe.akka"         %% "akka-persistence-testkit"  % akkaVersion % "test",
  "org.iq80.leveldb"          % "leveldb"                    % "0.9",
  "org.fusesource.leveldbjni" % "leveldbjni-all"             % "1.8",
  "com.github.dnvriend"       %% "akka-persistence-inmemory" % "2.5.15.2",
  "org.scalatest"             %% "scalatest"                 % "3.2.2" % "test",
  "ch.qos.logback"            % "logback-classic"            % "1.2.3"
)

// scalaFmt
scalafmtOnCompile := true

fork := true
