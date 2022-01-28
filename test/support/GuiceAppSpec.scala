/*
 * Copyright 2022 HM Revenue & Customs
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

package support

import connectors.NrsRetrievalConnector
import models.NotableEvent
import org.scalatest.Suite
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.{Injector, bind}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.typedmap.TypedKey
import play.api.test.FakeRequest
import play.api.{Application, Mode}
import play.filters.csrf.{CSRFConfigProvider, CSRFFilter}

import scala.collection.immutable

class GuiceAppSpec extends UnitSpec with GuiceOneAppPerSuite with PatienceConfiguration { this: Suite =>
  val nrsRetrievalConnector: NrsRetrievalConnector = mock[NrsRetrievalConnector]

  implicit override lazy val app: Application = {
    new GuiceApplicationBuilder().configure(Map(
      "google-analytics.host" -> "host",
      "google-analytics.token" -> "aToken"))
      .bindings(Seq(): _*).in(Mode.Test)
      .overrides(bind[NrsRetrievalConnector].toInstance(nrsRetrievalConnector))
      .build()
  }

  lazy val injector: Injector = app.injector

  implicit lazy val messagesApi: MessagesApi = injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(FakeRequest())

  lazy val indexedNotableEvents: immutable.Seq[(NotableEvent, Int)] =
    appConfig.notableEvents.values.toList.sortBy(_.displayName).zipWithIndex

  def addToken[T](fakeRequest: FakeRequest[T]): FakeRequest[T] = {
    val csrfConfig = injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter = injector.instanceOf[CSRFFilter]
    val token = csrfFilter.tokenProvider.generateToken

    fakeRequest
      .withAttrs(fakeRequest.attrs + (
        TypedKey("CSRF_TOKEN") -> token,
        TypedKey("CSRF_TOKEN_NAME") -> csrfConfig.tokenName))
      .withHeaders((csrfConfig.headerName, token))
  }
}
