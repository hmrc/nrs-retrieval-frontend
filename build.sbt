import uk.gov.hmrc.DefaultBuildSettings
import play.core.PlayVersion.current

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*AuthService.*;modgiels/.data/..*;controllers.auth.*;filters.*;forms.*;config.*;" +
      ".*BuildInfo.*;prod.Routes;app.Routes;testOnlyDoNotUseInAppConf.Routes;controllers.ExampleController;controllers.testonly.TestOnlyController",
    ScoverageKeys.coverageMinimumStmtTotal := 70.00,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

lazy val appDependencies: Seq[ModuleID] = compile ++ test()
lazy val appDependenciesIt: Seq[ModuleID] = it()
lazy val appName: String = "nrs-retrieval-frontend"
val currentScalaVersion = "3.5.0"
val bootstrapPlayVersion = "9.11.0"

lazy val compile = Seq(
  ws,
  "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapPlayVersion,
  "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % "11.12.0",
  "com.typesafe.play" %% "play-json-joda"             % "2.10.5",
  "commons-io"        %  "commons-io"                 % "2.17.0"
)
def test(scope: String = "test"): Seq[ModuleID] = Seq(
  "org.scalatest" %% "scalatest" % "3.2.18" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % scope,
  "org.playframework" %% "play-test" % current % scope,
  "org.scalatestplus" %% "mockito-4-11" % "3.2.18.0" % Test,
  "com.vladsch.flexmark" % "flexmark-all" % "0.64.8" % scope,
  "uk.gov.hmrc"       %% "bootstrap-test-play-30"% bootstrapPlayVersion % scope
)
def it(scope: String = "test"): Seq[ModuleID] = Seq(
  "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion % scope,
  "com.github.tomakehurst" % "wiremock" % "3.0.1" % scope,
  "org.playframework" %% "play-test" % current % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % scope,
  "com.vladsch.flexmark" % "flexmark-all" % "0.64.8" % scope
)

lazy val root = (project in file("."))
  .settings(
    name := appName,
    organization := "uk.gov.hmrc",
    PlayKeys.playDefaultPort := 9390,
    majorVersion := 0,
    scalaVersion := currentScalaVersion,
    ThisBuild / scalacOptions += "-Wconf:msg=unused imports&src=routes/.*:s",
    ThisBuild / scalacOptions += "-Wconf:msg=unused imports&src=html/.*:s",
    ThisBuild / scalacOptions += "-Wconf:msg=routes/.*:s",
    ThisBuild / scalacOptions += "-Wconf:msg=Flag.*repeatedly:s",
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
    ),
    libraryDependencies ++= appDependencies,
    scoverageSettings)
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.nrsretrievalfrontend.views.html.components._",
      "uk.gov.hmrc.nrsretrievalfrontend.controllers.routes",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    )
  )
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)

lazy val it = project
  .dependsOn(root % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .enablePlugins(play.sbt.PlayScala)
  .settings(scalaVersion := currentScalaVersion)
  .settings(majorVersion := 1)
  .settings(
    libraryDependencies ++= appDependenciesIt
  )