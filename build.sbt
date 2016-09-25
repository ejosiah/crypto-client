name := "crypto-client"

version := "0.1.0"

scalaVersion := "2.11.8"


libraryDependencies += "io.netty" % "netty-all" % "4.1.3.Final"
libraryDependencies += "com.typesafe" % "config" % "1.3.0"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21"
libraryDependencies +=   "crypto-utility" %% "crypto-utility-protocol" % "0.1.0" % Compile
libraryDependencies +=   "org.scalatest" %% "scalatest" % "3.0.0" % Test
libraryDependencies +=   "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % Test
libraryDependencies +=   "org.mockito" % "mockito-core" % "1.10.19" % Test

enablePlugins(JavaAppPackaging)

//enablePlugins(JavaServerAppPackaging)