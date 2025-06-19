/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.nrsretrievalfrontend.actions

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.{Result, Results}
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.{FakeRequest, ResultExtractors}
import uk.gov.hmrc.nrsretrievalfrontend.actions.requests.{AuthenticatedRequest, NotableEventRequest}
import uk.gov.hmrc.nrsretrievalfrontend.models.{NotableEvent, SearchKey}
import uk.gov.hmrc.nrsretrievalfrontend.support.{BaseUnitSpec, Views}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class NotableEventRefinerSpec extends BaseUnitSpec, MockitoSugar, Results, Status, ScalaFutures, HeaderNames, ResultExtractors, Views:

  trait Setup:

    def notableEventRefiner(notableEvent: String) = new NotableEventRefiner(
      messagesApi = messagesApi,
      errorPage = error_template
    )(notableEvent)

  "refining request based on notable event" should {
    "execute the block with correct notable event" when {
      "the event exists" in new Setup:
        val notableEvent = "vat-registration"

        val searchKey          = SearchKey(
          name = "postCodeOrFormBundleId",
          label = "Post Code or Form Bundle Id"
        )
        val notableEventConfig = NotableEvent(
          name = "vat-registration",
          displayName = "VAT registration",
          pluralDisplayName = "VAT registrations",
          storedFrom = "16 November 2020",
          storedFor = "20 years",
          searchKeys = List(searchKey),
          estimatedRetrievalTime = 15.minutes,
          crossKeySearch = true
        )

        val request = new AuthenticatedRequest("authProviderId", FakeRequest())

        val action: NotableEventRequest[?] => Future[Result] = request =>
          Future(Ok(Json.obj("notableEvent" -> Json.toJson(request.notableEvent), "searchKey" -> Json.toJson(request.searchKey))))

        val result: Future[Result] = notableEventRefiner(notableEvent).invokeBlock(request, action)

        status(result) shouldBe OK
        val json: JsValue = contentAsJson(result)

        (json \ "notableEvent").as[NotableEvent] shouldBe notableEventConfig
        (json \ "searchKey").as[SearchKey]       shouldBe searchKey
    }

    "return error page" when {
      "notable event does not exist" in new Setup:
        val notableEvent = "INVALID"

        val request = new AuthenticatedRequest("authProviderId", FakeRequest())

        val action: NotableEventRequest[?] => Future[Result] = request =>
          Future(Ok(Json.obj("notableEvent" -> Json.toJson(request.notableEvent), "searchKey" -> Json.toJson(request.searchKey))))

        val result: Future[Result] = notableEventRefiner(notableEvent).invokeBlock(request, action)

        status(result) shouldBe NOT_FOUND
        val doc: Document = Jsoup.parse(contentAsString(result))
        doc.title()                                        shouldBe "Not found"
        doc.select("#main-content > div > div > p").text() shouldBe "Notable Event Type not found"
    }
  }
