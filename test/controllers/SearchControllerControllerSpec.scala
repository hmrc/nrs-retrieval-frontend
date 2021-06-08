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

package controllers

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import config.AppConfig
import connectors.NrsRetrievalConnector
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import play.api.test.{FakeRequest, StubControllerComponentsFactory}
import play.api.{Configuration, Environment}
import support.fixtures.{NrsSearchFixture, SearchFixture, StrideFixture}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class SearchControllerControllerSpec extends UnitSpec
  with WithFakeApplication
  with MockitoSugar
  with SearchFixture
  with NrsSearchFixture
  with StrideFixture
  with StubControllerComponentsFactory {

  private val jsonBody: JsValue = Json.parse("""{"searchKeyName_0": "someValue", "searchKeyValue_0": "someValue", "notableEventType": "vat-return"}""")
  private implicit val fakeRequest: FakeRequest[AnyContentAsJson] = FakeRequest("GET", "/").withJsonBody(jsonBody)

  private val env = Environment.simple()
  private val configuration = Configuration.load(env)

  implicit val appConfig: AppConfig = new AppConfig(configuration, env, new ServicesConfig(configuration))
  private val mockActorRef = mock[ActorRef]
  private val mockNRC = mock[NrsRetrievalConnector]
  implicit val mockSystem: ActorSystem = mock[ActorSystem]
  implicit val mockMaterializer: Materializer = mock[Materializer]
  private val searchPage = fakeApplication.injector.instanceOf[views.html.search_page]
  private val errorPage = fakeApplication.injector.instanceOf[views.html.error_template]

  private class TestControllerAuthSearch(stubbedRetrievalResult: Future[_])
    extends SearchController(mockActorRef, authConnector, mockNRC, searchResultUtils, stubMessagesControllerComponents(), searchPage, errorPage) {

    override val authConnector: AuthConnector = authConnOk(stubbedRetrievalResult)
  }

  private val controller = new TestControllerAuthSearch(authResultOk)


  "showSearchPage" should {
    "return 200" in {
      val result = controller.showSearchPage(notableEventType = "vat-return")(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }

  "searchForm" should {
    "return no errors for valid data" in {
      val postData = Json.obj("searchText" -> "someSearchText",
        "notableEventType" -> "vat-return")
      val validatedForm = FormMappings.searchForm.bind(postData)
      validatedForm.errors shouldBe empty
    }
    "create a header carrier with X-API-Key when one exists in config" in {
      controller.hc.extraHeaders should contain("X-API-Key" -> appConfig.xApiKey)
    }
  }
}


