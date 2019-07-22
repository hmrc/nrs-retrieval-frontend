import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "nrs-retrieval-frontend"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    "uk.gov.hmrc" %% "govuk-template" % "5.26.0-play-25",
    "uk.gov.hmrc" %% "play-ui" % "7.27.0-play-25",
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % "4.11.0",
    "uk.gov.hmrc" %% "auth-client" % "2.6.0"
  )

  def test(scope: String = "test") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.3.0" % scope,
    "org.scalatest" %% "scalatest" % "2.2.6" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "org.jsoup" % "jsoup" % "1.8.1" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.mockito" % "mockito-all" % "2.0.2-beta" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope,
    "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % scope)

}
