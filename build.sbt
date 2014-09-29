name := "akka_pools"

version := "1.4_SNAPSHOT"

scalaVersion := "2.10.4"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies +=  "com.typesafe.akka" %% "akka-actor" % "2.3.6"

libraryDependencies += "com.typesafe.akka" %% "akka-contrib" % "2.3.6"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.3.6"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.3.6"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.6"

libraryDependencies += "com.typesafe.akka" %% "akka-kernel" % "2.3.6"

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.3.6"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.0" % "runtime"

scalacOptions += "-deprecation"
