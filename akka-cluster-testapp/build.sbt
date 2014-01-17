import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.SbtNativePackager._

val akkaVersion = "2.3.9"

name := "akka-cluster-testapp"

organization := "com.typesafe.akka"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.5"

scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.6", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint")

javacOptions in Compile ++= Seq("-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-Xlint:deprecation")

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
    "com.typesafe.akka" %% "akka-kernel" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.0.7",
    "org.fusesource" % "sigar" % "1.6.4",
    "com.amazonaws" % "aws-java-sdk" % "1.4.2.1")
      
mainClass in (Compile, run) := Some("testapp.Main")

packageArchetype.java_server

mappings in Universal ++= {
  ((file("src/main/resources") * "*").get map { f => f -> ("conf/" + f.name) }) ++
  ((file("sigar") * "*").get map { f => f -> ("lib/" + f.name) }) ++
  ((file("bin") * "*").get map { f => f -> ("bin/" + f.name) })
}

scriptClasspath := Seq("../conf/") ++ scriptClasspath.value


