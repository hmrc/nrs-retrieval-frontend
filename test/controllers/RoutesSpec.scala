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

import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.{AhcWSResponse, StandaloneAhcWSResponse}
import play.api.libs.ws.ahc.cache.CacheableResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClientConfig
import support.fixtures.NrsSearchFixture
import support.{BaseSpec, GuiceAppSpec}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

class RoutesSpec extends GuiceAppSpec with BaseSpec with NrsSearchFixture {

  "GET /start" should {
    "show the start page" in {
      val result: Option[Future[Result]] = route(app, FakeRequest(GET, "/nrs-retrieval/start"))

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
      text should not include Messages("search.results.notfound.lbl")
      text should not include Messages("search.results.results.lbl")
    }
  }

  "POST /search" should {
    val notableEventType = "vat-return"
    val request = FakeRequest(POST, s"/nrs-retrieval/search/$notableEventType")

    "show the search page" when {
      "the search returns no results" in {
        when(mockNrsRetrievalConnector.search(any(), any())(any())).thenReturn(Future.successful(Seq.empty))

        val result: Option[Future[Result]] = route(app, addToken(request.withFormUrlEncodedBody(
          ("notableEventType", notableEventType)
        )))

        result.map(status(_)) shouldBe Some(OK)

        val text: String = result.map(contentAsString(_)).get
        text should include(Messages("search.results.notfound.lbl"))
      }

      "the search returns results" in {
        when(mockNrsRetrievalConnector.search(any(), any())(any())).thenReturn(Future.successful(Seq(nrsVatSearchResult)))
        val result: Option[Future[Result]] = route(app, addToken(request.withFormUrlEncodedBody(
          ("searchKeyName_0", "vrn"),
          ("searchKeyValue_0", "noResults"),
          ("notableEventType", notableEventType)
        )))

        result.map(status(_)) shouldBe Some(OK)

        val text: String = result.map(contentAsString(_)).get
        text should include(Messages(s"search.page.$notableEventType.header.lbl"))
        text should not include Messages("search.results.notfound.lbl")
        text should include(Messages("search.results.results.lbl"))
      }
    }

    Seq(BAD_REQUEST, BAD_GATEWAY).foreach{ statusCode =>
      "show an error page" when {
        s"$statusCode response from the upstream search service" in {
          when(mockNrsRetrievalConnector.search(any(), any())(any())).thenReturn(
            Future.failed(UpstreamErrorResponse("Broken", statusCode, statusCode)))

          val result: Option[Future[Result]] = route(app, addToken(request.withFormUrlEncodedBody(
            ("action", "search"),
            ("query.searchText", "results")
          )))

          result.get.onComplete {
            case Failure(e: UpstreamErrorResponse) if e.statusCode == statusCode => succeed
            case _ => fail
          }
        }
      }
    }
  }

  "GET /download/:vaultId/:archiveId" should {
    val request = FakeRequest(GET, "/nrs-retrieval/download/1/2")

    "return OK and the file contents" in {
      val fileContents = "file contents"
      val mockWsResponse = mock[WSResponse]
      when(mockWsResponse.headers).thenReturn(Map("One" -> Seq("Two")))

      val response =
        AhcWSResponse(
          StandaloneAhcWSResponse(
            CacheableResponse(OK, "http://www.test.com", fileContents, mock[AsyncHttpClientConfig])))

      when(mockNrsRetrievalConnector.getSubmissionBundle(any(), any(), any())(any())).thenReturn(Future.successful(response))
      val result: Option[Future[Result]] = route(app, request)

      result.map(status(_)) shouldBe Some(OK)
      result.map(contentAsString(_)) shouldBe Some(fileContents)
    }

    Seq(BAD_REQUEST, BAD_GATEWAY).foreach { statusCode =>
      "error" when {
        s"$statusCode response from the upstream download service" in {
          when(mockNrsRetrievalConnector.getSubmissionBundle(any(), any(), any())(any())).thenReturn(
            Future.failed(UpstreamErrorResponse("Broken", statusCode, statusCode)))

          val result: Option[Future[Result]] = route(app, addToken(request))

          result.get.onComplete {
            case Failure(e: UpstreamErrorResponse) if e.statusCode == statusCode => succeed
            case _ => fail
          }
        }
      }
    }
  }

  Seq(BAD_REQUEST, BAD_GATEWAY).foreach { statusCode =>
    "GET /status/:vaultId/:archiveId" should {
      "show an error page" when {
        s"$statusCode response from the upstream status service" in {
          when(mockNrsRetrievalConnector.getSubmissionBundle(any(), any(), any())(any())).thenReturn(
            Future.failed(UpstreamErrorResponse("Broken", statusCode, statusCode)))

          val result: Option[Future[Result]] = route(app, addToken(FakeRequest(GET, "/nrs-retrieval/status/1/2")))

          result.get.onComplete {
            case Failure(e: UpstreamErrorResponse) if e.statusCode == statusCode => succeed
            case _ => fail
          }
        }
      }
    }
  }
}