val Akka = "2.5.2"
val AkkaCom = "1.0.3"

lazy val apps = project
  .in(file("."))
  .aggregate(ddata, sharding)

lazy val ddata = project
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka"     %% "akka-cluster-sharding"     % Akka,
      "com.typesafe.akka"     %% "akka-distributed-data"     % Akka,
      "com.lightbend.akka"    %% "akka-split-brain-resolver" % AkkaCom,
      "com.lightbend.akka"    %% "akka-diagnostics"          % AkkaCom,
      "com.github.romix.akka" %% "akka-kryo-serialization"   % "0.5.1",
      "org.hdrhistogram"       % "HdrHistogram"              % "2.1.9"
    )
  )
  .enablePlugins(JavaAppPackaging)

lazy val sharding = project.enablePlugins(ddata.plugins)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka"     %% "akka-cluster-sharding"        % Akka,
      "com.typesafe.akka"     %% "akka-persistence"             % Akka,
      "org.hdrhistogram"       % "HdrHistogram"                 % "2.1.9",
      "com.typesafe.akka"     %% "akka-persistence-cassandra"   % "0.54",
      "com.github.romix.akka" %% "akka-kryo-serialization"      % "0.5.1",
      "com.lightbend.akka"    %% "akka-management-cluster-http" % "0.3"

    )
  )

inThisBuild(Seq(
  scalaVersion := "2.12.2",

  scalafmtVersion := "1.0.0-RC3",
  scalafmtOnCompile := true,

  // headers
  organizationName := "Lightbend Inc.",
  startYear := Some(2017),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
))
