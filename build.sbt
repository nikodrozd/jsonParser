
lazy val root = (project in file(".")).
  settings(
    name := "jsonParser",
    version := "1.0",
    scalaVersion := "2.13.6",
    Compile / mainClass := Some("Main")
//    Compile / resourceDirectory := {baseDirectory.value / "src/main/resources"}
  )

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.4.0",
  "com.h2database" % "h2" % "1.4.197",
  "com.typesafe.play" %% "play-json" % "2.9.2"
)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
