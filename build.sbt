import play.core.PlayVersion
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*AuthService.*;modgiels/.data/..*;controllers.auth.*;filters.*;forms.*;config.*;" +
      ".*BuildInfo.*;prod.Routes;app.Routes;testOnlyDoNotUseInAppConf.Routes;controllers.ExampleController;controllers.testonly.TestOnlyController",
    ScoverageKeys.coverageMinimum := 70.00,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

lazy val compile = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "2.24.0",
  "uk.gov.hmrc" %% "play-ui" % "8.11.0-play-27",
  "uk.gov.hmrc" %% "govuk-template" % "5.55.0-play-27",
  "uk.gov.hmrc" %% "auth-client" % "3.0.0-play-27",
  "com.typesafe.play" %% "play-json-joda" % "2.9.0",
  "com.typesafe.akka" %% "akka-stream" % "2.6.8",
  "com.typesafe.akka" %% "akka-slf4j" % "2.6.8"
)

def test(scope: String) = Seq(
  "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % scope,
  "org.scalatest" %% "scalatest" % "3.0.8" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
  "com.typesafe.akka" %% "akka-testkit" % "2.6.8" % scope,
  "org.mockito" % "mockito-all" % "2.0.2-beta" % scope,
  "org.jsoup" % "jsoup" % "1.13.1" % scope
)

lazy val appName: String = "nrs-retrieval-frontend"

val silencerVersion = "1.7.1"

lazy val root = (project in file("."))
  .settings(
    name := appName,
    organization := "uk.gov.hmrc",
    PlayKeys.playDefaultPort := 9390,
    majorVersion := 0,
    scalaVersion := "2.12.12",
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.bintrayRepo("hmrc", "release-candidates"),
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    ),
    libraryDependencies ++= compile ++ test("test") ++ test("it"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    ),
    scalacOptions += "-P:silencer:pathFilters=target/.*",
    publishingSettings,
    scoverageSettings)
  .configs(IntegrationTest)
  .settings(
    Keys.fork in IntegrationTest := false,
    Defaults.itSettings,
    unmanagedSourceDirectories in IntegrationTest += baseDirectory(_ / "it").value,
    parallelExecution in IntegrationTest := false
  )
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .disablePlugins(JUnitXmlReportPlugin)
inConfig(IntegrationTest)(Defaults.itSettings)