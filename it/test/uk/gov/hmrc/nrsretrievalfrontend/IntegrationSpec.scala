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
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.*
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Binding, Injector, Module}
import play.api.libs.ws.WSClient
import play.api.mvc.{MessagesControllerComponents, RequestHeader}
import play.api.{Application, Configuration, Environment, inject}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.nrsretrievalfrontend.actions.AuthenticatedAction
import uk.gov.hmrc.nrsretrievalfrontend.config.AppConfig
import uk.gov.hmrc.nrsretrievalfrontend.connectors.NrsRetrievalConnectorImpl
import uk.gov.hmrc.nrsretrievalfrontend.views.html.error_template
import uk.gov.hmrc.nrsretrievalfrontend.wiremock.WireMockSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait IntegrationSpec
    extends AnyWordSpec, Matchers, ScalaFutures, GuiceOneServerPerSuite, WireMockSupport, IntegrationPatience, BeforeAndAfterEach, Fixture:

  val authenticationHeader: (String, String) = "Authorization" -> "Bearer some-token"
  override def beforeEach(): Unit            = WireMock.reset()

  override def fakeApplication(): Application =
    GuiceApplicationBuilder(
      modules = Seq(
        new Module():
          override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
            Seq(inject.bind[AuthenticatedAction].to[ITAuthenticatedAction])
      )
    )
      .configure(configuration)
      .build()

  val defaultConfiguration: Map[String, Any] = Map[String, Any](
    "microservice.services.auth.port"          -> wireMockPort,
    "microservice.services.nrs-retrieval.port" -> wireMockPort,
    "play.filters.csrf.header.bypassHeaders"   -> Map(
      "Authorization" -> "Bearer some-token"
    ),
    "auditing.enabled"                         -> false,
    "metrics.jvm"                              -> false
  )

  def configuration: Map[String, Any] = defaultConfiguration

  lazy val injector: Injector                   = fakeApplication().injector
  lazy val wsClient: WSClient                   = injector.instanceOf[WSClient]
  lazy val httpClientV2: HttpClientV2           = injector.instanceOf[HttpClientV2]
  lazy val connector: NrsRetrievalConnectorImpl = injector.instanceOf[NrsRetrievalConnectorImpl]
  given hc: HeaderCarrier                       = HeaderCarrier()

  lazy val serviceRoot = s"http://localhost:$port/nrs-retrieval"

@Singleton
class ITAuthenticatedAction @Inject() (
  authConnector: AuthConnector,
  config: Configuration,
  env: Environment,
  controllerComponents: MessagesControllerComponents,
  errorPage: error_template
)(using executionContext: ExecutionContext, appConfig: AppConfig)
    extends AuthenticatedAction(authConnector, config, env, controllerComponents, errorPage):

  // Override the hc method to make use of full action
  override protected def hc(using request: RequestHeader): HeaderCarrier =
    HeaderCarrierConverter.fromRequest(request)
