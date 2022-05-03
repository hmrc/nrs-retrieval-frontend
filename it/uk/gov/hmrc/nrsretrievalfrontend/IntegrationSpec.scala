/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.nrsretrievalfrontend

import com.github.tomakehurst.wiremock.client.WireMock
import controllers.StrideAuthSettings
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Binding, Injector, Module}
import play.api.libs.ws.WSClient
import play.api.{Application, Configuration, Environment, inject}
import uk.gov.hmrc.nrsretrievalfrontend.wiremock.WireMockSupport

trait IntegrationSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with GuiceOneServerPerSuite
  with WireMockSupport
  with IntegrationPatience
  with BeforeAndAfterEach
  with Fixture {
  override def beforeEach(): Unit = WireMock.reset()

  override def fakeApplication(): Application =
    GuiceApplicationBuilder(
      modules =
        Seq(
          new Module() {
            override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
              Seq(inject.bind[StrideAuthSettings].to[StrideAuthIsDisabled])
          }
        )
    )
    .configure(configuration)
    .build()

  val defaultConfiguration: Map[String, Any] = Map[String, Any](
    "microservice.services.nrs-retrieval.port" -> wireMockPort,
    "auditing.enabled" -> false,
    "metrics.jvm" -> false,
    "stride.enabled" -> false)

  def configuration: Map[String, Any] = defaultConfiguration

  lazy val injector: Injector = fakeApplication().injector
  lazy val wsClient: WSClient = injector.instanceOf[WSClient]

  lazy val serviceRoot = s"http://localhost:$port/nrs-retrieval"
}

class StrideAuthIsDisabled extends StrideAuthSettings {
  override val strideAuthEnabled: Boolean = false
}