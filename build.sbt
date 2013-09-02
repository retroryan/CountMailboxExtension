organization := "typesafe"

name := "CountMyMail"


//Most of the buid configuration is in Build.scala

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"


seq(ScctPlugin.instrumentSettings : _*)

parallelExecution in Test := false

parallelExecution in ScctTest := false
