import AssemblyKeys._ // put this at the top of the file

assemblySettings

jarName in assembly := "akkastuffs.jar"

name := "akka_pools"

version := "1.4_SNAPSHOT"

scalaVersion := "2.10.1"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies +=  "com.typesafe.akka" %% "akka-actor" % "2.2.0-RC1"

libraryDependencies += "com.typesafe.akka" %% "akka-contrib" % "2.2.0-RC1"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.2.0-RC1"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.2.0-RC1"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.2.0-RC1"

libraryDependencies += "com.typesafe.akka" %% "akka-kernel" % "2.2.0-RC1"

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.2.0-RC1"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.0" % "runtime"

scalacOptions += "-deprecation"
