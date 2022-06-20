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

package controllers

import akka.actor.ActorRef
import akka.util.ByteString
import models.{NotableEvent, SearchResultUtils}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.mockito.internal.stubbing.answers.Returns
import play.api.i18n.Messages
import play.api.libs.json.Json.parse
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{headers, status, _}
import support.fixtures.{NrsSearchFixture, SearchFixture}
import uk.gov.hmrc.govukfrontend.views.html.components.{FormWithCSRF, GovukButton, GovukErrorMessage, GovukHint, GovukInput, GovukLabel}
import views.html.components.{Button, Paragraph, SearchResultPanel, SearchResultsPanel, TextInput}
import views.html.search_page

import scala.concurrent.Future

class SearchControllerSpec extends ControllerSpec with SearchFixture with NrsSearchFixture {

  val searchPage = new search_page(
    layout,
    new FormWithCSRF,
    new Paragraph,
    new TextInput(new GovukInput(new GovukErrorMessage, new GovukHint, new GovukLabel)),
    new Button(new GovukButton),
    new SearchResultsPanel(new SearchResultPanel(new Paragraph))
  )


  private val controller =
    new SearchController(
      mock[ActorRef],
      mockAuthConnector,
      nrsRetrievalConnector,
      new SearchResultUtils(appConfig),
      stubMessagesControllerComponents(),
      new StrideAuthSettings(),
      searchPage,
      error_template)

  private def theSearchPageShouldBeRenderedWithoutResults(eventualResult: Future[Result], notableEvent: NotableEvent) = {
    val content = aPageShouldBeRendered(eventualResult, Messages("search.page.dynamic.header.lbl", notableEvent.displayName))

    Option(content.getElementById("notFound")).isDefined shouldBe false
    Option(content.getElementById("resultsFound")).isDefined shouldBe false
  }

  private def getRequestWithJsonBody(notableEventType: String) =
    getRequest.withJsonBody(
      parse(s"""{"searchKeyName_0": "someValue", "searchKeyValue_0": "someValue", "notableEventType": "$notableEventType"}"""))

  s"showSearchPage" should {
    indexedNotableEvents.foreach { case (notableEvent, _) =>
      val notableEventType = notableEvent.name

      "return 200 and render the search page" when {
        s"$notableEventType is specified and and the request is authorised" in {
          givenTheRequestIsAuthorised()
          theSearchPageShouldBeRenderedWithoutResults(
            controller.showSearchPage(notableEventType)(getRequestWithJsonBody(notableEventType)), notableEvent)
        }
      }
    }

    "return OK and render the error page" when {
      "and the request is unauthorised" in {
        givenTheRequestIsUnauthorised()
        theNotAuthorisedPageShouldBeRendered(
          controller.showSearchPage("vatReturn")(getRequestWithJsonBody("vatReturn")))
      }
    }
  }

  "noParameters" should {
    "redirect to the selector page" when {
      def theRequestShouldBeRedirectedToTheStartPage(eventualResult: Future[Result]) = {
        status(eventualResult) shouldBe SEE_OTHER
        headers(eventualResult) shouldBe Map("Location" -> controllers.routes.StartController.showStartPage.url)
      }

      "and the request is authorised" in {
        givenTheRequestIsAuthorised()
        theRequestShouldBeRedirectedToTheStartPage(controller.noParameters()(getRequest))
      }

      "and the request is unauthorised" in {
        givenTheRequestIsUnauthorised()
        theRequestShouldBeRedirectedToTheStartPage(controller.noParameters()(getRequest))
      }
    }
  }

  "submitSearchPage" should {
    indexedNotableEvents.foreach { case (notableEvent, _) =>
      val notableEventType = notableEvent.name

      val postRequestWithNotableEventTypeAndSearchText =
        FakeRequest("POST", "/")
          .withFormUrlEncodedBody(("notableEventType", notableEventType), ("searchText", "someSearchText"))

      def givenTheSearchSucceeds() =
        when(nrsRetrievalConnector.search(any(), any(), Matchers.eq(notableEvent.crossKeySearch))(any()))
          .thenAnswer(new Returns(Future.successful(Seq(nrsVatSearchResult))))

      def theSearchPageShouldBeRenderedWithResults(eventualResult: Future[Result], notableEvent: NotableEvent) = {
        val content = aPageShouldBeRendered(eventualResult, Messages("search.page.dynamic.header.lbl", notableEvent.displayName))

        Option(content.getElementById("notFound")).isDefined shouldBe false
        Option(content.getElementById("resultsFound")).isDefined shouldBe true
      }

      "perform a search and render the results" when {
        s"and the request is authorised and a $notableEventType search is submitted" in {
          givenTheRequestIsAuthorised()
          givenTheSearchSucceeds()

          theSearchPageShouldBeRenderedWithResults(
            controller.submitSearchPage(notableEventType)(postRequestWithNotableEventTypeAndSearchText), notableEvent)
        }

        s"a $notableEventType search is submitted with no search text" in {
          givenTheSearchSucceedsWithNoResults()

          theSearchPageShouldBeRenderedWithEmptyResults(
            controller
              .submitSearchPage(notableEventType)(FakeRequest("POST", "/")
                .withFormUrlEncodedBody(("notableEventType", notableEventType))), notableEvent)

          def givenTheSearchSucceedsWithNoResults() =
            when(nrsRetrievalConnector.search(any(), any(), any())(any()))
              .thenAnswer(new Returns(Future.successful(Seq.empty)))

          def theSearchPageShouldBeRenderedWithEmptyResults(eventualResult: Future[Result], notableEvent: NotableEvent) = {
            val content = aPageShouldBeRendered(eventualResult, Messages("search.page.dynamic.header.lbl", notableEvent.displayName))

            Option(content.getElementById("notFound")).isDefined shouldBe true
            Option(content.getElementById("resultsFound")).isDefined shouldBe false
          }
        }
      }
    }

    "return OK and render the error page" when {
      "and the request is unauthorised" in {
        givenTheRequestIsUnauthorised()
        theNotAuthorisedPageShouldBeRendered(
          controller.submitSearchPage("vatReturn")(emptyPostRequest))
      }
    }
  }

  "download" should {
    val vaultName = "vat-return"
    val archiveId = "vrn"

    "return 200 and a byte stream" when {
      def givenTheDownloadSucceeds() = {
        val mockWSResponse = mock[WSResponse]

        when(nrsRetrievalConnector.getSubmissionBundle(any(), any(), any())(any()))
          .thenAnswer(new Returns(Future.successful(mockWSResponse)))
        when(mockWSResponse.headers).thenAnswer(new Returns(Map.empty))
        when(mockWSResponse.bodyAsBytes).thenReturn(ByteString("Some zipped bytes"))
      }

      def theDownloadedBytesShouldBeReturned(eventualResponse: Future[Result]) = {
        status(eventualResponse) shouldBe OK
        contentAsString(eventualResponse) shouldBe "Some zipped bytes"
      }

      "and the request is authorised" in {
        givenTheRequestIsAuthorised()
        givenTheDownloadSucceeds()
        theDownloadedBytesShouldBeReturned(controller.download(vaultName, archiveId)(getRequest))
      }
    }

    "return OK and render the error page" when {
      "and the request is unauthorised" in {
        givenTheRequestIsUnauthorised()
        theNotAuthorisedPageShouldBeRendered(controller.download(vaultName, archiveId)(getRequest))
      }
    }
  }
}


