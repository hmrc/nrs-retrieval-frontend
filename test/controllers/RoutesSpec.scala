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

import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.fixtures.NrsSearchFixture
import support.{BaseSpec, GuiceAppSpec}
import uk.gov.hmrc.http.{Upstream4xxResponse, Upstream5xxResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

class RoutesSpec extends GuiceAppSpec with BaseSpec with NrsSearchFixture {

  "GET /start" should {
    "show the start page" in {
      var result: Option[Future[Result]] = route(app, FakeRequest(GET, "/nrs-retrieval/start"))

      result.map(status(_)) shouldBe Some(OK)

      val text: String = result.map(contentAsString(_)).get
      text should include(Messages("start.page.header.lbl"))
    }
  }

  "GET /search" should {
    "show the search empty search page" in {
      val notableEventType: String = "vat-return"
      val result: Option[Future[Result]] = route(app, FakeRequest(GET, "/nrs-retrieval/search/" + notableEventType))

      result.map(status(_)) shouldBe Some(OK)

      val text: String = result.map(contentAsString(_)).get
      text should include(Messages(s"search.page.$notableEventType.header.lbl"))
      text should not include(Messages("search.results.notfound.lbl"))
      text should not include(Messages("search.results.results.lbl"))
    }
  }

  "POST /search" should {
    "show the search page" when {
      val notableEventType: String = "vat-return"
      "the search returns no results" in {
        when(mockNrsRetrievalConnector.search(any())(any())).thenReturn(Future.successful(Seq.empty))

        val result: Option[Future[Result]] = route(app, addToken(FakeRequest(POST, s"/nrs-retrieval/search/$notableEventType").withFormUrlEncodedBody(
          ("notableEventType", notableEventType)
        )))

        result.map(status(_)) shouldBe Some(OK)

        val text: String = result.map(contentAsString(_)).get
        text should include(Messages("search.results.notfound.lbl"))
      }

      "the search returns results" in {
        when(mockNrsRetrievalConnector.search(any())(any())).thenReturn(Future.successful(Seq(nrsVatSearchResult)))
        val result: Option[Future[Result]] = route(app, addToken(FakeRequest(POST, s"/nrs-retrieval/search/$notableEventType").withFormUrlEncodedBody(
          ("searchKeyName_0", "vrn"),
          ("searchKeyValue_0", "noResults"),
          ("notableEventType", notableEventType)
        )))

        result.map(status(_)) shouldBe Some(OK)

        val text: String = result.map(contentAsString(_)).get
        text should include(Messages(s"search.page.$notableEventType.header.lbl"))
        text should not include (Messages("search.results.notfound.lbl"))
        text should include(Messages("search.results.results.lbl"))
      }
    }

    "show an error page" when {
      "5xx response from the upstream search service" in {
        when(mockNrsRetrievalConnector.search(any())(any())).thenReturn(Future.failed(new Upstream5xxResponse("Broken", 502, 502)))

        val result: Option[Future[Result]] = route(app, addToken(FakeRequest(POST, "/nrs-retrieval/search").withFormUrlEncodedBody(
          ("action", "search"),
          ("query.searchText", "results")
        )))

        result.get.onComplete {
          case Failure(e) if e.isInstanceOf[Upstream5xxResponse] => succeed
          case _ => fail
        }
      }

      "4xx response from the upstream search service" in {
        when(mockNrsRetrievalConnector.search(any())(any())).thenReturn(Future.failed(new Upstream4xxResponse("Broken", 502, 502)))

        val result: Option[Future[Result]] = route(app, addToken(FakeRequest(POST, "/nrs-retrieval/search").withFormUrlEncodedBody(
          ("action", "search"),
          ("query.searchText", "results")
        )))

        result.get.onComplete {
          case Failure(e) if e.isInstanceOf[Upstream5xxResponse] => succeed
          case _ => fail
        }
      }
    }
  }

  "GET /download/:vaultId/:archiveId" should {
    "return OK" in {
      val mockWsResponse = mock[WSResponse]
      when(mockWsResponse.allHeaders).thenReturn(Map("One" -> Seq("Two")))

      when(mockNrsRetrievalConnector.getSubmissionBundle(any(), any())(any())).thenReturn(Future.successful(mockWsResponse))
      val result: Option[Future[Result]] = route(app, FakeRequest(GET, "/nrs-retrieval/download/1/2"))

      result.map(status(_)) shouldBe Some(OK)
    }
    "show an error page" when {
      "5xx response from the upstream download service" in {
        when(mockNrsRetrievalConnector.getSubmissionBundle(any(), any())(any())).thenReturn(Future.failed(new Upstream5xxResponse("Broken", 502, 502)))

        val result: Option[Future[Result]] = route(app, addToken(FakeRequest(GET, "/nrs-retrieval/download/1/2")))

        result.get.onComplete {
          case Failure(e) if e.isInstanceOf[Upstream5xxResponse] => succeed
          case _ => fail
        }
      }
      "4xx response from the upstream download service" in {
        when(mockNrsRetrievalConnector.getSubmissionBundle(any(), any())(any())).thenReturn(Future.failed(new Upstream4xxResponse("Broken", 502, 502)))

        val result: Option[Future[Result]] = route(app, addToken(FakeRequest(GET, "/nrs-retrieval/download/1/2")))

        result.get.onComplete {
          case Failure(e) if e.isInstanceOf[Upstream5xxResponse] => succeed
          case _ => fail
        }
      }
    }
  }

  "GET /status/:vaultId/:archiveId" should {
    "show an error page" when {
      "5xx response from the upstream status service" in {
        when(mockNrsRetrievalConnector.getSubmissionBundle(any(), any())(any())).thenReturn(Future.failed(new Upstream5xxResponse("Broken", 502, 502)))

        val result: Option[Future[Result]] = route(app, addToken(FakeRequest(GET, "/nrs-retrieval/status/1/2")))

        result.get.onComplete {
          case Failure(e) if e.isInstanceOf[Upstream5xxResponse] => succeed
          case _ => fail
        }
      }
      "4xx response from the upstream status service" in {
        when(mockNrsRetrievalConnector.getSubmissionBundle(any(), any())(any())).thenReturn(Future.failed(new Upstream4xxResponse("Broken", 502, 502)))

        val result: Option[Future[Result]] = route(app, addToken(FakeRequest(GET, "/nrs-retrieval/status/1/2")))

        result.get.onComplete {
          case Failure(e) if e.isInstanceOf[Upstream5xxResponse] => succeed
          case _ => fail
        }
      }
    }
  }




}