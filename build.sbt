val Akka = "2.5.8"
val AkkaHttp = "10.0.10"
val AkkaCom = "1.1-M5"
val AkkaClusterManagement = "0.5"
val AkkaPersistenceCassandra = "0.57"
val KryoVersion = "0.5.0"
val HdrHistogramVersion = "2.1.9"

lazy val `apps-root` = project
  .in(file("."))
  .aggregate(ddata)
  .aggregate(pubsub)
//  .enablePlugins(ScalafmtPlugin)

lazy val commonPlugins = List( JavaAppPackaging)

lazy val ddata = project.enablePlugins(commonPlugins: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka"     %% "akka-cluster-sharding"        % Akka,
      "com.typesafe.akka"     %% "akka-persistence"             % Akka,
      "com.lightbend.akka"    %% "akka-split-brain-resolver"    % AkkaCom,
      "com.lightbend.akka"    %% "akka-diagnostics"             % AkkaCom,
      "org.hdrhistogram"       % "HdrHistogram"                 % HdrHistogramVersion,
      "com.typesafe.akka"     %% "akka-persistence-cassandra"   % AkkaPersistenceCassandra,
      "com.github.romix.akka" %% "akka-kryo-serialization"      % KryoVersion,
      "com.lightbend.akka"    %% "akka-management-cluster-http" % AkkaClusterManagement,
      "com.typesafe.akka"     %% "akka-http"                    % AkkaHttp
    )
  )

lazy val sharding = project.enablePlugins(commonPlugins: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka"     %% "akka-cluster-sharding"        % Akka,
      "com.typesafe.akka"     %% "akka-persistence"             % Akka,
      "org.hdrhistogram"       % "HdrHistogram"                 % HdrHistogramVersion,
      "com.typesafe.akka"     %% "akka-persistence-cassandra"   % AkkaPersistenceCassandra,
      "com.github.romix.akka" %% "akka-kryo-serialization"      % KryoVersion,
      "com.lightbend.akka"    %% "akka-management-cluster-http" % AkkaClusterManagement

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
      "com.github.romix.akka" %% "akka-kryo-serialization"      % KryoVersion,
      "org.hdrhistogram"       % "HdrHistogram"                 % HdrHistogramVersion,
      "com.lightbend.akka"    %% "akka-management-cluster-http" % AkkaClusterManagement,
      "com.typesafe.akka"     %% "akka-http"                    % AkkaHttp,
      "com.typesafe.akka"     %% "akka-http-spray-json"         % AkkaHttp
    )
  )


lazy val multidc = project
  .enablePlugins(commonPlugins: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka"     %% "akka-persistence-cassandra"   % AkkaPersistenceCassandra,
      "com.lightbend.akka"    %% "akka-persistence-multi-dc"    % AkkaCom,
      "com.typesafe.akka"     %% "akka-cluster-sharding"        % Akka,
      "com.typesafe.akka"     %% "akka-distributed-data"        % Akka,
      "com.lightbend.akka"    %% "akka-split-brain-resolver"    % AkkaCom,
      "com.lightbend.akka"    %% "akka-diagnostics"             % AkkaCom,
      "com.github.romix.akka" %% "akka-kryo-serialization"      % KryoVersion,
      "org.hdrhistogram"       % "HdrHistogram"                 % HdrHistogramVersion,
      "com.lightbend.akka"    %% "akka-management-cluster-http" % AkkaClusterManagement,
      "com.typesafe.akka"     %% "akka-http"                    % AkkaHttp,
      "com.typesafe.akka"     %% "akka-slf4j"                   % Akka,
      "com.typesafe.akka"     %% "akka-http-spray-json"         % AkkaHttp,
      "ch.qos.logback"         % "logback-classic"              % "1.2.3"
    )
  )

inThisBuild(Seq(
  scalaVersion := "2.11.11",
  //scalafmtVersion := "1.0.0-RC2",
  test in assembly := {},

  // headers
  organizationName := "Lightbend Inc.",
  startYear := Some(2017),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
))
