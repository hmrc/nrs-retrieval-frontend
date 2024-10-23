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
val currentScalaVersion = "2.13.12"
val bootstrapPlayVersion = "8.5.0"

lazy val compile = Seq(
  ws,
  "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapPlayVersion,
  "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % "8.5.0",
  "com.typesafe.play" %% "play-json-joda"             % "2.10.4",
  "commons-io"        %  "commons-io"                 % "2.15.1"
)
def test(scope: String = "test"): Seq[ModuleID] = Seq(
  "org.scalatest" %% "scalatest" % "3.2.17" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % scope,
  "org.playframework" %% "play-test" % current % scope,
  "org.scalatestplus" %% "mockito-1-10" % "3.1.0.0" % Test,
  "com.vladsch.flexmark" % "flexmark-all" % "0.64.8" % scope,
  "uk.gov.hmrc"       %% "bootstrap-test-play-30"% bootstrapPlayVersion % scope
)
def it(scope: String = "test"): Seq[ModuleID] = Seq(
  "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion % scope,
  "com.github.tomakehurst" % "wiremock" % "2.33.2" % scope,
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
    scalacOptions += "-Wconf:cat=deprecation:warning-verbose",
    scalacOptions += "-Wconf:cat=unused-imports&src=routes/.*:s",
    scalacOptions += "-Wconf:cat=unused-imports&src=html/.*:s",
    scalacOptions += "-Wconf:src=routes/.*:s",
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
  .settings(majorVersion := 0)
  .settings(
    libraryDependencies ++= appDependenciesIt
  )