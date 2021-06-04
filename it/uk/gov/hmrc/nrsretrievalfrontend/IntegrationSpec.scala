package uk.gov.hmrc.nrsretrievalfrontend

import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.nrsretrievalfrontend.wiremock.WireMockSupport

trait IntegrationSpec extends WordSpecLike
  with Matchers
  with ScalaFutures
  with GuiceOneServerPerSuite
  with WireMockSupport
  with IntegrationPatience
  with BeforeAndAfterEach {
  override def beforeEach(): Unit = WireMock.reset()

  override def fakeApplication(): Application = new GuiceApplicationBuilder().configure(configuration).build()

  val defaultConfiguration: Map[String, Any] = Map[String, Any](
    "microservice.services.nrs-retrieval.port" -> wireMockPort,
    "auditing.enabled" -> false,
    "metrics.jvm" -> false)

  def configuration: Map[String, Any] = defaultConfiguration

  lazy val injector: Injector = fakeApplication().injector
}
