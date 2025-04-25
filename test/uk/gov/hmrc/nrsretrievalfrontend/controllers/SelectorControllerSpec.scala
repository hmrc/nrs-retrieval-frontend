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

import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.concurrent.Future

class SelectorControllerSpec extends ControllerSpec:

  private def selectorController =
    new SelectorController(
      authenticatedAction,
      stubMessagesControllerComponents(),
      selectorPage
    )

  "showSelectorPage" should {
    def theSelectorPageShouldBeRendered(eventualResult: Future[Result]) =
      aPageShouldBeRendered(eventualResult, messages("selector.page.header.lbl"))

    "return 200 and render the selector page" when {
      "the request is authorised" in
        theSelectorPageShouldBeRendered(selectorController.showSelectorPage(getRequest))
    }
  }

  "submitSelectorPage" should {
    "redirect to the search page" when
      indexedNotableEvents.foreach { case (notableEvent, _) =>
        val notableEventType                = notableEvent.name
        val postRequestWithNotableEventType = FakeRequest("POST", "/").withFormUrlEncodedBody(("notableEventType", notableEventType))

        def theRequestShouldBeRedirectedToTheSearchPage(eventualResult: Future[Result]) =
          status(eventualResult)  shouldBe SEE_OTHER
          headers(eventualResult) shouldBe
            Map("Location" -> routes.SearchController.showSearchPage(notableEventType).url)

        s"and the request is authorised and the notable event type $notableEventType is selected" in
          theRequestShouldBeRedirectedToTheSearchPage(selectorController.submitSelectorPage(postRequestWithNotableEventType))
      }

    "return 200 and render the selector page with an error message" when {
      def theSelectorPageShouldBeRenderedWithAnErrorMessage(eventualResult: Future[Result]) =
        aPageShouldBeRendered(eventualResult, messages("generic.errorPrefix") + " " + messages("selector.page.header.lbl"))

      "and the request is authorised and no notable event type is selected" in
        theSelectorPageShouldBeRenderedWithAnErrorMessage(selectorController.submitSelectorPage(emptyPostRequest))
    }
  }
