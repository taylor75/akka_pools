import AssemblyKeys._ // put this at the top of the file

assemblySettings

jarName in assembly := "akkastuffs.jar"

name := "akka_pools"

version := "1.0"

scalaVersion := "2.10.0"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" % "akka-actor_2.10" % "2.1.4"

libraryDependencies += "com.typesafe.akka" % "akka-remote_2.10" % "2.1.4"

libraryDependencies += "com.typesafe.akka" % "akka-slf4j_2.10" % "2.1.4"

libraryDependencies += "com.typesafe.akka" % "akka-testkit_2.10" % "2.1.4"

libraryDependencies += "com.typesafe.akka" % "akka-kernel_2.10" % "2.1.4"

scalacOptions += "-deprecation"

