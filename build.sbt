// Chisel versions must match the wildcat submodule (see wildcat/build.sbt)
ThisBuild / scalaVersion := "2.13.14"
val chiselVersion = "3.6.1"

// wildcat is built from the submodule using its own build.sbt
lazy val wildcat = project in file("wildcat")

lazy val root = (project in file("."))
  .dependsOn(wildcat)
  .settings(
    name := "wafer-space-explore",
    scalacOptions ++= Seq(
      "-feature",
      "-language:reflectiveCalls",
    ),
    addCompilerPlugin("edu.berkeley.cs" %% "chisel3-plugin" % chiselVersion cross CrossVersion.full),
    libraryDependencies += "edu.berkeley.cs" %% "chisel3" % chiselVersion,
    libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.6.2" % Test,
    libraryDependencies += "com.github.scopt" %% "scopt" % "4.1.0",
    // Chisel sources live next to the SystemVerilog in src/
    Compile / scalaSource := baseDirectory.value / "src" / "scala",
    // Run in a separate JVM, so sys.exit in the generators does not stop sbt
    fork := true,
  )
