/*
 * Copyright 2018 HM Revenue & Customs
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
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.i18n.{DefaultLangs, DefaultMessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import support.fixtures.{NrsSearchFixture, SearchFixture, StrideFixture}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class SearchControllerControllerSpec extends UnitSpec with WithFakeApplication with MockitoSugar with SearchFixture with NrsSearchFixture with StrideFixture {

  private val jsonBody: JsValue = Json.parse("""{"searchKeyName_0": "someValue", "searchKeyValue_0": "someValue", "notableEventType": "vat-return"}""")
  private implicit val fakeRequest: FakeRequest[AnyContentAsJson] = FakeRequest("GET", "/").withJsonBody(jsonBody)

  private val env = Environment.simple()
  private val configuration = Configuration.load(env)

  private val messageApi = new DefaultMessagesApi(env, configuration, new DefaultLangs(configuration))
  private val appConfig = new AppConfig(configuration, env)
  private val mockAcorRef = mock[ActorRef]
  private val mockNRC = mock[NrsRetrievalConnector]
  implicit val mockSystem: ActorSystem = mock[ActorSystem]
  implicit val mockMaterializer: Materializer = mock[Materializer]

  private class TestControllerAuthSearch(stubbedRetrievalResult: Future[_])
    extends SearchController(messageApi, mockAcorRef, appConfig, mockAuthConn, mockNRC, mockSystem, mockMaterializer, searchResultUtils) {

    override val authConnector = authConnOk(stubbedRetrievalResult)

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
      controller.hc.headers should contain ("X-API-Key" -> appConfig.xApiKey)
    }
  }

}


