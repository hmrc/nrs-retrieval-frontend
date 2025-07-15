/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.nrsretrievalfrontend.controllers

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.internal.stubbing.answers.Returns
import play.api.i18n.Messages
import play.api.libs.json.Json.parse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.nrsretrievalfrontend.models.{AuthorisedUser, NotableEvent, SearchResultUtils}
import uk.gov.hmrc.nrsretrievalfrontend.support.fixtures.{NrsSearchFixture, SearchFixture}

import scala.concurrent.Future

class SearchControllerSpec extends ControllerSpec, SearchFixture, NrsSearchFixture:

  private val controller =
    new SearchController(
      authenticatedAction,
      notableEventRefiner,
      nrsRetrievalConnector,
      new SearchResultUtils(appConfig),
      stubMessagesControllerComponents(),
      ActorSystem(),
      searchPage,
      error_template
    )

  private def theSearchPageShouldBeRenderedWithoutResults(eventualResult: Future[Result], notableEvent: NotableEvent) =
    val content = aPageShouldBeRendered(eventualResult, Messages("search.page.dynamic.header.lbl", notableEvent.pluralDisplayName))

    Option(content.getElementById("notFound")).isDefined     shouldBe false
    Option(content.getElementById("resultsFound")).isDefined shouldBe false

  private def getRequestWithJsonBody(notableEventType: String) =
    getRequest.withJsonBody(
      parse(s"""{"searchKeyName_0": "someValue", "searchKeyValue_0": "someValue", "notableEventType": "$notableEventType"}""")
    )

  s"showSearchPage" should
    indexedNotableEvents.filterNot(_._1.metadataSearchKeys).foreach { case (notableEvent, _) =>
      val notableEventType = notableEvent.name

      "return 200 and render the search page" when {
        s"$notableEventType is specified and and the request is authorised" in
          theSearchPageShouldBeRenderedWithoutResults(
            controller.showSearchPage(notableEventType)(getRequestWithJsonBody(notableEventType)),
            notableEvent
          )
      }
    }

  "noParameters" should {
    "redirect to the selector page" when {
      def theRequestShouldBeRedirectedToTheStartPage(eventualResult: Future[Result]) =
        status(eventualResult)  shouldBe SEE_OTHER
        headers(eventualResult) shouldBe Map("Location" -> routes.StartController.showStartPage.url)

      "and the request is authorised" in
        theRequestShouldBeRedirectedToTheStartPage(controller.noParameters()(getRequest))

      "and the request is unauthorised" in
        theRequestShouldBeRedirectedToTheStartPage(controller.noParameters()(getRequest))
    }
  }

  "submitSearchPage" should
    indexedNotableEvents.filterNot(_._1.metadataSearchKeys).foreach { case (notableEvent, _) =>
      val notableEventType = notableEvent.name

      val postRequestWithNotableEventTypeAndSearchText =
        FakeRequest("POST", "/")
          .withFormUrlEncodedBody(
            ("notableEventType", notableEventType),
            ("queries[0].name", "nino"),
            ("queries[0].value", "validNino2024")
          )

      def givenTheSearchSucceeds() =
        when(
          nrsRetrievalConnector.search(any(), any(), ArgumentMatchers.eq(notableEvent.crossKeySearch))(using
            any[HeaderCarrier],
            any[AuthorisedUser]
          )
        )
          .thenAnswer(new Returns(Future.successful(Seq(nrsVatSearchResult))))
        when(
          nrsRetrievalConnector.metaSearch(any(), any())(using
            any[HeaderCarrier],
            any[AuthorisedUser]
          )
        )
          .thenAnswer(new Returns(Future.successful(Seq(nrsVatSearchResult))))

      def theSearchPageShouldBeRenderedWithResults(eventualResult: Future[Result], notableEvent: NotableEvent) =
        val content = aPageShouldBeRendered(eventualResult, Messages("search.page.dynamic.header.lbl", notableEvent.pluralDisplayName))

        Option(content.getElementById("notFound")).isDefined     shouldBe false
        Option(content.getElementById("resultsFound")).isDefined shouldBe true

      "perform a search and render the results" when {
        s"and the request is authorised and a $notableEventType search is submitted" in {
          givenTheSearchSucceeds()

          theSearchPageShouldBeRenderedWithResults(
            controller.submitSearchPage(notableEventType)(postRequestWithNotableEventTypeAndSearchText),
            notableEvent
          )
        }

        s"a $notableEventType search is submitted with no search text" in {
          givenTheSearchSucceedsWithNoResults()

          theSearchPageShouldBeRenderedWithEmptyResults(
            controller
              .submitSearchPage(notableEventType)(
                FakeRequest("POST", "/")
                  .withFormUrlEncodedBody(
                    ("notableEventType", notableEventType),
                    ("queries[0].name", "nino"),
                    ("queries[0].value", "validNino2024")
                  )
              ),
            notableEvent
          )

          def givenTheSearchSucceedsWithNoResults() =
            when(nrsRetrievalConnector.search(any(), any(), any())(using any[HeaderCarrier], any[AuthorisedUser]))
              .thenAnswer(new Returns(Future.successful(Seq.empty)))
            when(nrsRetrievalConnector.metaSearch(any(), any())(using any[HeaderCarrier], any[AuthorisedUser]))
              .thenAnswer(new Returns(Future.successful(Seq.empty)))

          def theSearchPageShouldBeRenderedWithEmptyResults(eventualResult: Future[Result], notableEvent: NotableEvent) =
            val content = aPageShouldBeRendered(eventualResult, Messages("search.page.dynamic.header.lbl", notableEvent.pluralDisplayName))

            Option(content.getElementById("notFound")).isDefined     shouldBe true
            Option(content.getElementById("resultsFound")).isDefined shouldBe false
        }
      }
    }

  "download" should {
    val notableEvent = "vat-return"
    val vaultName    = "vat-return"
    val archiveId    = "vrn"

    "return 200 and a byte stream" when {
      def givenTheDownloadSucceeds() =
        val mockHttpResponse = mock[HttpResponse]

        when(nrsRetrievalConnector.getSubmissionBundle(any(), any())(using any[HeaderCarrier], any[AuthorisedUser]))
          .thenReturn(Future.successful(mockHttpResponse))
        when(mockHttpResponse.headers).thenReturn(
          Map(
            "content-length" -> Seq("15"),
            "content-type"   -> Seq("application/zip")
          )
        )
        when(mockHttpResponse.bodyAsSource).thenReturn(Source.single(ByteString("Some zipped bytes")))

      def theDownloadedBytesShouldBeReturned(eventualResponse: Future[Result]) =
        status(eventualResponse)          shouldBe OK
        contentAsString(eventualResponse) shouldBe "Some zipped bytes"

      "and the request is authorised" in {
        givenTheDownloadSucceeds()
        theDownloadedBytesShouldBeReturned(controller.download(notableEvent, vaultName, archiveId)(getRequest))
      }
    }
  }
