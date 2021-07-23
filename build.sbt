import play.core.PlayVersion
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, integrationTestSettings}
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
  "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % "5.7.0",
  "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "0.84.0-play-28",
  "com.typesafe.play" %% "play-json-joda"             % "2.9.2",
)

def test(scope: String) = Seq(
  "com.github.tomakehurst" % "wiremock-jre8" % "2.23.2" % scope,
  "org.scalatest" %% "scalatest" % "3.2.9" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
  "org.scalatestplus"      %% "mockito-1-10"           % "3.1.0.0" % Test,
  "org.jsoup" % "jsoup" % "1.13.1" % scope,
  "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % scope,
  "com.typesafe.akka" %% "akka-testkit" % "2.6.14",
  "com.typesafe.akka" %% "akka-stream"                % "2.6.14",
  "com.typesafe.akka" %% "akka-slf4j"                 % "2.6.14",
  "com.typesafe.akka" %% "akka-actor-typed"           % "2.6.14",
  "com.typesafe.akka" %% "akka-serialization-jackson" % "2.6.14",
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
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
    ),
    libraryDependencies ++= compile ++ test("test") ++ test("it"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    ),
    scalacOptions += "-P:silencer:pathFilters=target/.*",
    publishingSettings,
    scoverageSettings)
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.govukfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    )
  )
  .settings(defaultSettings(): _*)
  .settings(integrationTestSettings())
  .configs(IntegrationTest)
  .settings(
    Keys.fork in IntegrationTest := false,
    Defaults.itSettings,
    unmanagedSourceDirectories in IntegrationTest += baseDirectory(_ / "it").value,
    parallelExecution in IntegrationTest := false
  )
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .disablePlugins(JUnitXmlReportPlugin)
