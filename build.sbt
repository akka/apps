val Akka = "2.5.2"

lazy val `apps-root` = project
  .in(file("."))
  .aggregate(apps)
  //.enablePlugins(ScalafmtPlugin)

lazy val apps = project
  .enablePlugins(
    AutomateHeaderPlugin,
    ScalafmtPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster-sharding" % Akka,
      "com.typesafe.akka" %% "akka-distributed-data" % Akka
    )
  )

lazy val sharding = project.enablePlugins(apps.plugins)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster-sharding" % Akka,
      "com.typesafe.akka" %% "akka-persistence" % Akka,
      "org.hdrhistogram" % "HdrHistogram" % "2.1.9",
      "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.54",
      "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.1"
    )
  )

inThisBuild(Seq(
  scalaVersion := "2.12.2",
  scalafmtVersion := "1.0.0-RC1",

  // headers
  organizationName := "Lightbend Inc.",
  startYear := Some(2017),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
))
