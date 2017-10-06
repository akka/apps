val Akka = "2.5.6"
val AkkaHttp = "10.0.9"
val AkkaCom = "1.0.3"

lazy val `apps-root` = project
  .in(file("."))
  .aggregate(ddata)
  .aggregate(pubsub)
  .enablePlugins(ScalafmtPlugin)

lazy val commonPlugins = List(AutomateHeaderPlugin, ScalafmtPlugin, JavaAppPackaging)

lazy val ddata = project.enablePlugins(commonPlugins: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka"     %% "akka-cluster-sharding"        % Akka,
      "com.typesafe.akka"     %% "akka-persistence"             % Akka,
      "com.lightbend.akka"    %% "akka-split-brain-resolver"    % AkkaCom,
      "com.lightbend.akka"    %% "akka-diagnostics"             % AkkaCom,
      "org.hdrhistogram"       % "HdrHistogram"                 % "2.1.9",
      "com.typesafe.akka"     %% "akka-persistence-cassandra"   % "0.54",
      "com.github.romix.akka" %% "akka-kryo-serialization"      % "0.5.1",
      "com.lightbend.akka"    %% "akka-management-cluster-http" % "0.3",
      "com.typesafe.akka"     %% "akka-http"                    % AkkaHttp
    )
  )

lazy val sharding = project.enablePlugins(commonPlugins: _*)
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

lazy val pubsub = project
  .enablePlugins(commonPlugins: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka"     %% "akka-cluster-sharding"        % Akka,
      "com.typesafe.akka"     %% "akka-distributed-data"        % Akka,
      "com.lightbend.akka"    %% "akka-split-brain-resolver"    % AkkaCom,
      "com.lightbend.akka"    %% "akka-diagnostics"             % AkkaCom,
      "com.github.romix.akka" %% "akka-kryo-serialization"      % "0.5.1",
      "org.hdrhistogram"       % "HdrHistogram"                 % "2.1.9",
      "com.lightbend.akka"    %% "akka-management-cluster-http" % "0.3",
      "com.typesafe.akka"     %% "akka-http"                    % AkkaHttp,
      "com.typesafe.akka"     %% "akka-http-spray-json"         % AkkaHttp
    )
  )


inThisBuild(Seq(
  scalaVersion := "2.12.2",
  scalafmtVersion := "1.0.0-RC2",

  // headers
  organizationName := "Lightbend Inc.",
  startYear := Some(2017),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
))
