name := "crypto-client"

version := "0.1.0"

scalaVersion := "2.11.8"

maintainer := "Josiah <joebhomenye@gmail.com>"
packageSummary := "crypto-client"
packageDescription := """crypto-client MSI."""

// wix build information
wixProductId := "ce07be71-510d-414a-92d4-dff47631848a"
wixProductUpgradeId := "4552fb0e-e257-4dbd-9ecb-dba9dbacf424"


libraryDependencies += "io.netty" % "netty-all" % "4.1.3.Final"
libraryDependencies += "com.typesafe" % "config" % "1.3.0"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21"
libraryDependencies +=   "crypto-utility" %% "crypto-utility-protocol" % "0.1.0" % Compile
libraryDependencies +=   "org.scalatest" %% "scalatest" % "3.0.0" % Test
libraryDependencies +=   "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % Test
libraryDependencies +=   "org.mockito" % "mockito-core" % "1.10.19" % Test

enablePlugins(JavaAppPackaging)

//enablePlugins(JavaServerAppPackaging)