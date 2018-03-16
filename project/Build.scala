import sbt._
import Keys._
import java.io.File

// import com.trueaccord.scalapb.{ScalaPbPlugin => PB}

object SlotsBuild extends Build {

	val appName = "slots"
	val appVersion = "1.0"

	val localPath = Path.userHome + File.separator + ".ivy2" + File.separator + "local"

	val appDependencies = Seq(
      "io.netty" % "netty-all" % "4.1.1.Final"
    , "com.typesafe.akka" %% "akka-actor" % "2.4.7"
    , "com.thesamet.scalapb" %% "compilerplugin" % "0.7.0"
	)

    lazy val setup = Defaults.defaultSettings ++ /* PB.protobufSettings ++*/ Seq(
		scalaVersion := "2.11.7"
	,	version := appVersion
	,	libraryDependencies ++= appDependencies
	,	resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
	,	resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"
	,	resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
    )

	lazy val main = Project(appName, file(".")).settings(setup.toArray:_*)
    
}
