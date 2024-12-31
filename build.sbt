import xerial.sbt.Sonatype.*

name := "pekko-quartz-scheduler"

organization := "io.github.samueleresca"

version := "1.3.0-pekko-1.1.x"

val Scala212Version = "2.12.20"
val Scala213Version = "2.13.15"
val Scala3Version = "3.3.3"
val PekkoVersion = "1.1.2"

ThisBuild / scalaVersion := Scala213Version
ThisBuild / crossScalaVersions := Seq(Scala212Version, Scala213Version, Scala3Version)
ThisBuild / scalacOptions ++= Seq("-language:postfixOps")

libraryDependencies ++= Seq(
  "org.apache.pekko"     %% "pekko-actor"       % PekkoVersion,
  "org.apache.pekko"     %% "pekko-actor-typed" % PekkoVersion,
  ("org.quartz-scheduler" % "quartz"            % "2.3.2")
    .exclude("com.zaxxer", "HikariCP-java7"),
  "org.apache.pekko" %% "pekko-testkit"             % PekkoVersion % Test,
  "org.apache.pekko" %% "pekko-actor-testkit-typed" % PekkoVersion % Test,
  "junit"             % "junit"                     % "4.13.2"     % Test,
  "org.slf4j"         % "slf4j-api"                 % "2.0.16"     % Test,
  "org.slf4j"         % "slf4j-jcl"                 % "1.7.36"     % Test,
  "org.scalatest"    %% "scalatest"                 % "3.2.19"     % Test
)

// Sonatype release settings
pomIncludeRepository := { _ => false }
sonatypeCredentialHost := "s01.oss.sonatype.org"
publishTo := sonatypePublishToBundle.value
sonatypeProjectHosting := Some(
  GitHubHosting(user = "samueleresca", repository = "pekko-quartz-scheduler", email = "samuele.resca@gmail.com")
)
// Metadata referrsing to licenses, website, and SCM (source code management)
licenses := Seq("APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
sonatypeProfileName := "io.github.samueleresca"
publishMavenStyle := true
scmInfo := Some(
  ScmInfo(
    url("https://github.com/samueleresca/pekko-quartz-scheduler"),
    "scm:git@github.com:samueleresca/pekko-quartz-scheduler.git"
  )
)

ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test")))

ThisBuild / githubWorkflowTargetTags ++= Seq("*.*.*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq.empty
ThisBuild / githubWorkflowPublish := Seq.empty

ThisBuild / githubWorkflowOSes := Seq("ubuntu-latest", "macos-latest")

// sbt is not preinstalled in macos-latest
ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Run(
    List("brew install sbt"),
    name = Some("Install missing sbt"),
    cond = Some("matrix.os == 'macos-latest'")
  )

ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.corretto("8"), // Use corretto because temurin doesn't provide a JDK 1.8 supporting Apple M1.
  JavaSpec.temurin("11"),
  JavaSpec.temurin("17"),
  JavaSpec.temurin("21")
)
