/*
 * Copyright (C) 2016-2019 Cafienne B.V. <https://www.cafienne.io/bounded>
 */

import sbt._

object Dependencies {

  val akkaVersion = "2.6.4"
  val staminaVersion = "0.1.5"
  val persistenceInMemVersion = "2.5.15.2"
  val scalaTestVersion = "3.1.1"

  val baseDeps = {
    def akkaModule(name: String, version: String = akkaVersion) =
      "com.typesafe.akka" %% s"akka-$name" % version
    Seq(
      akkaModule("slf4j"),
      akkaModule("actor"),
      akkaModule("stream"),
      akkaModule("persistence"),
      akkaModule("persistence-typed"),
      akkaModule("persistence-query"),
      akkaModule("cluster-sharding-typed"),
      akkaModule("cluster"),
      akkaModule("coordination"),
      akkaModule("cluster-tools"),
      akkaModule("stream-testkit") % Test,
      akkaModule("testkit") % Test,
      akkaModule("actor-testkit-typed") % Test,
      "com.scalapenos"              %% "stamina-json"                           % staminaVersion,
      "io.spray"                    %% "spray-json"                             % "1.3.5",
      "com.github.dnvriend"         %% "akka-persistence-inmemory"              % persistenceInMemVersion,
      "com.typesafe.scala-logging"  %% "scala-logging"                          % "3.9.2"
    )
  }

  val log = Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "net.logstash.logback" % "logstash-logback-encoder" % "6.3"
    )


  val  test = log ++ Seq(
      "org.scalatest"       %% "scalatest"                   % scalaTestVersion % Test,
      "com.scalapenos"      %% "stamina-testkit"             % staminaVersion % Test,
      "org.scalamock"       %% "scalamock-scalatest-support" % "3.6.0" % Test
    )


  val akkaHttpDeps = {
    def akkaHttpModule(name: String, version: String = "10.1.11") =
      "com.typesafe.akka" %% s"akka-$name" % version

    baseDeps ++ Seq(
      akkaHttpModule("http"),
      akkaHttpModule("http-spray-json"),
      akkaHttpModule("http-testkit") % Test,
      "io.swagger" % "swagger-jaxrs" % "1.6.1",
      // As suggested in https://stackoverflow.com/questions/43574426/how-to-resolve-java-lang-noclassdeffounderror-javax-xml-bind-jaxbexception-in-j
      // to resolve blow-up due to swagger :  java.lang.NoClassDefFoundError: javax/xml/bind/annotation/XmlRootElement.
      "javax.xml.bind" % "jaxb-api" % "2.3.1",
      "com.github.swagger-akka-http" %% "swagger-akka-http" % "2.0.4"
    ) ++ test
  }

  val testDeps = {
    baseDeps ++ Seq(
      "org.scalatest"          %% "scalatest"                   % scalaTestVersion,
      "com.typesafe.akka"      %% "akka-testkit"                % akkaVersion
    ) ++ test
  }

  val persistenceLevelDBDeps = {
    baseDeps ++ Seq(
      "org.iq80.leveldb"            % "leveldb"        % "0.9",
      "org.fusesource.leveldbjni"   % "leveldbjni-all" % "1.8"
    )
  }

  val persistanceLmdbDBDeps = {
    baseDeps ++ Seq(
      "org.lmdbjava"                % "lmdbjava"        % "0.7.0"
    )
  }

  val persistenceCassandraDeps = {
    baseDeps ++ Seq(
      "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.103"
    )
  }

}
