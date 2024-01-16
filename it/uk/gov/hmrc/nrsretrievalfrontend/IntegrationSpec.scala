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
import org.scalatest.matchers.should._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.{Application, Configuration, Environment, inject}
import play.api.inject.{Binding, Injector, Module}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc.{MessagesControllerComponents, RequestHeader}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nrsretrievalfrontend.actions.AuthenticatedAction
import uk.gov.hmrc.nrsretrievalfrontend.config.AppConfig
import uk.gov.hmrc.nrsretrievalfrontend.views.html.error_template
import uk.gov.hmrc.nrsretrievalfrontend.wiremock.WireMockSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait IntegrationSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with GuiceOneServerPerSuite
  with WireMockSupport
  with IntegrationPatience
  with BeforeAndAfterEach
  with Fixture {

  val authenticationHeader: (String, String) = "Authorization" -> "Bearer some-token"
  override def beforeEach(): Unit = WireMock.reset()

  override def fakeApplication(): Application =
    GuiceApplicationBuilder(
      modules =
        Seq(
          new Module() {
            override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
              Seq(inject.bind[AuthenticatedAction].to[ITAuthenticatedAction])
          }
        )
    )
    .configure(configuration)
    .build()

  val defaultConfiguration: Map[String, Any] = Map[String, Any](
    "microservice.services.auth.port" -> wireMockPort,
    "microservice.services.nrs-retrieval.port" -> wireMockPort,
    "bootstrap.filters.csrf.enabled" -> false,
    "auditing.enabled" -> false,
    "metrics.jvm" -> false)

  def configuration: Map[String, Any] = defaultConfiguration

  lazy val injector: Injector = fakeApplication().injector
  lazy val wsClient: WSClient = injector.instanceOf[WSClient]

  lazy val serviceRoot = s"http://localhost:$port/nrs-retrieval"
}

@Singleton
class ITAuthenticatedAction @Inject()(
             authConnector: AuthConnector,
             config: Configuration,
             env: Environment,
             controllerComponents: MessagesControllerComponents,
             errorPage: error_template
           )(implicit executionContext: ExecutionContext, appConfig: AppConfig)
  extends AuthenticatedAction(authConnector, config, env, controllerComponents, errorPage) {

  //Override the hc method to make use of full action
  override implicit protected def hc(implicit request: RequestHeader): HeaderCarrier =
    HeaderCarrierConverter.fromRequest(request)
}