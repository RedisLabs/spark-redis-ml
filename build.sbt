// Your sbt build file. Guides on how to write one can be found at
// http://www.scala-sbt.org/0.13/docs/index.html

scalaVersion := "2.11.6"

sparkVersion := "2.0.0"

spName := "RedisLabs/spark-redis"

description := "A spark package for loading Spark ML models to Redis-ML"

// Don't forget to set the version
version := "0.1.0"

homepage := Some(url("https://github.com/RedisLabs/spark-redis"))

// All Spark Packages need a license
licenses := Seq("BSD 3-Clause" -> url("http://opensource.org/licenses/BSD-3-Clause"))

organization := "com.redislabs"

organizationName := "Redis Labs, Inc."

organizationHomepage := Some(url("https://redislabs.com"))

// Add Spark components this package depends on, e.g, "mllib", ....
sparkComponents ++= Seq("core", "mllib")
//sparkComponents ++= Seq("core")

//libraryDependencies ++= Seq( "redis.clients" % "jedis" % "2.7.2")

