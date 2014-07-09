name := "crate-scala"

organization := "uk.co.panaxiom"

version := "1.0"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "io.crate" % "crate-client" % "0.39.3",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test"
)

resolvers += "Crate bintray" at "http://dl.bintray.com/crate/crate"