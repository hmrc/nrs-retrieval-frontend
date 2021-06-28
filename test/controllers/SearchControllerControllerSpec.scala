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
import akka.util.Timeout
import config.AppConfig
import connectors.NrsRetrievalConnector
import controllers.FormMappings.searchForm
import models.{AuthorisedUser, SearchQuery}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, StubControllerComponentsFactory}
import play.api.{Configuration, Environment}
import support.fixtures.{NrsSearchFixture, SearchFixture, StrideFixture}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, SECONDS}

class SearchControllerControllerSpec extends UnitSpec
  with WithFakeApplication
  with MockitoSugar
  with SearchFixture
  with NrsSearchFixture
  with StrideFixture
  with Status
  with StubControllerComponentsFactory {

  private val notableEventType = "vat-return"
  private val jsonBody: JsValue =
    Json.parse("""{"searchKeyName_0": "someValue", "searchKeyValue_0": "someValue", "notableEventType": """" + notableEventType + """"}""")

  private val env = Environment.simple()
  private val configuration = Configuration.load(env)

  private implicit val fakeRequest: FakeRequest[AnyContentAsJson] = FakeRequest("GET", "/").withJsonBody(jsonBody)
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  private implicit val appConfig: AppConfig = new AppConfig(configuration, env, new ServicesConfig(configuration))
  private implicit val timeout: Timeout = new Timeout(new FiniteDuration(2, SECONDS))
  private implicit val mockSystem: ActorSystem = mock[ActorSystem]
  private implicit val mockMaterializer: Materializer = mock[Materializer]

  private val mockActorRef = mock[ActorRef]
  private val mockNRC = mock[NrsRetrievalConnector]

  private lazy val searchPage = fakeApplication.injector.instanceOf[views.html.search_page]
  private lazy val errorPage = fakeApplication.injector.instanceOf[views.html.error_template]

  private class TestControllerAuthSearch(stubbedRetrievalResult: Future[_])
    extends SearchController(mockActorRef, authConnector, mockNRC, searchResultUtils, stubMessagesControllerComponents(), searchPage, errorPage) {

    override val authConnector: AuthConnector = authConnOk(stubbedRetrievalResult)
  }

  private val controller = new TestControllerAuthSearch(authResultOk)


  "showSearchPage" should {
    "return 200" in {
      val result = controller.showSearchPage(notableEventType = notableEventType)(fakeRequest)
      status(result) shouldBe OK
    }
  }

  "submitSearchPage" should {
    "display results with a download attribute containing a valid zip file name" when {
      "results are returned" in {
        when(mockNRC.search(any[SearchQuery], any[AuthorisedUser])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Seq(nrsVatSearchResult)))

        val eventualResult =
          controller.submitSearchPage(notableEventType = notableEventType)(
            fakeRequest.withFormUrlEncodedBody("searchText" -> "someSearchText", "notableEventType" -> notableEventType))
        status(eventualResult) shouldBe OK
        val body = contentAsString(eventualResult)
        body.contains(s"""download="$nrSubmissionId.zip"""") shouldBe true
      }
    }
  }

  "searchForm" should {
    "return no errors for valid data" in {
      val postData = Json.obj("searchText" -> "someSearchText", "notableEventType" -> notableEventType)
      searchForm.bind(postData, Int.MaxValue).errors shouldBe empty
    }

    "create a header carrier with X-API-Key when one exists in config" in {
      controller.hc.extraHeaders should contain("X-API-Key" -> appConfig.xApiKey)
    }
  }
}


