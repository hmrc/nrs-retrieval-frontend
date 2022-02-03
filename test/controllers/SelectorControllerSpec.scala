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

import config.AppConfig
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.selector_page

import scala.concurrent.Future

class SelectorControllerSpec extends ControllerSpec {
  private def selectorController(implicit appConfig: AppConfig) =
    new SelectorController(
      mockAuthConnector, stubMessagesControllerComponents(), injector.instanceOf[selector_page], error_template)

  "showSelectorPage" should {
    def theSelectorPageShouldBeRendered(eventualResult: Future[Result]) =
      aPageShouldBeRendered(eventualResult, "selector.page.header.lbl")

    "return 200 and render the selector page" when {
      "auth is disabled" in {
        theSelectorPageShouldBeRendered(selectorController(appConfig).showSelectorPage(getRequest))
      }

      "auth is enabled and the request is authorised" in {
        givenTheRequestIsAuthorised()
        theSelectorPageShouldBeRendered(selectorController(authEnabledAppConfig).showSelectorPage(getRequest))
      }
    }

    "return OK and render the error page" when {
      "auth is enabled and the request is unauthorised" in {
        givenTheRequestIsUnauthorised()
        theNotAuthorisedPageShouldBeRendered(selectorController(authEnabledAppConfig).showSelectorPage(getRequest))
      }
    }
  }

  "submitSelectorPage" should {
    "redirect to the search page" when {
      indexedNotableEvents.foreach { case (notableEvent, _) =>
        val notableEventType = notableEvent.name
        val postRequestWithNotableEventType = FakeRequest("POST", "/").withFormUrlEncodedBody(("notableEventType", notableEventType))

        def theRequestShouldBeRedirectedToTheSearchPage(eventualResult: Future[Result]) = {
          status(eventualResult) shouldBe SEE_OTHER
          headers(eventualResult) shouldBe
            Map("Location" -> controllers.routes.SearchController.showSearchPage(notableEventType).url)
        }

        s"auth is disabled and the notable event type $notableEventType is selected" in {
          theRequestShouldBeRedirectedToTheSearchPage(
            selectorController(appConfig).submitSelectorPage(postRequestWithNotableEventType))
        }

        s"auth is enabled and the request is authorised and the notable event type $notableEventType is selected" in {
          givenTheRequestIsAuthorised()
          theRequestShouldBeRedirectedToTheSearchPage(
            selectorController(authEnabledAppConfig).submitSelectorPage(postRequestWithNotableEventType))
        }
      }
    }

    "return 200 and render the selector page with an error message" when {
      def theSelectorPageShouldBeRenderedWithAnErrorMessage(eventualResult: Future[Result]) =
        aPageShouldBeRendered(eventualResult, "generic.errorPrefix selector.page.header.lbl")

      "auth is disabled and no notable event type is selected" in {
        theSelectorPageShouldBeRenderedWithAnErrorMessage(
          selectorController(appConfig).submitSelectorPage(emptyPostRequest))
      }

      "auth is enabled and the request is authorised and no notable event type is selected" in {
        givenTheRequestIsAuthorised()
        theSelectorPageShouldBeRenderedWithAnErrorMessage(
          selectorController(authEnabledAppConfig).submitSelectorPage(emptyPostRequest))
      }
    }

    "return OK and render the error page" when {
      "auth is enabled and the request is unauthorised" in {
        givenTheRequestIsUnauthorised()
        theNotAuthorisedPageShouldBeRendered(selectorController(authEnabledAppConfig).submitSelectorPage(emptyPostRequest))
      }
    }
  }
}

