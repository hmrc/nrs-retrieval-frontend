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

import scala.concurrent.Future

class StartControllerSpec extends ControllerSpec {

  private def startController =
    new StartController(
      authenticatedAction,
      stubMessagesControllerComponents(),
      startPage
    )

  "showStartPage" should {
    "return 200 and render the start page" when {
      def theStartPageShouldBeRendered(eventualResult: Future[Result]) =
        aPageShouldBeRendered(eventualResult, messages("start.page.header.lbl"))

      "the request is authorised" in {
        theStartPageShouldBeRendered(startController.showStartPage(getRequest))
      }
    }

    "return OK and render the error page" when {
      "the request is unauthorised" in {
        theNotAuthorisedPageShouldBeRendered(startController.showStartPage(getRequest))
      }
    }
  }
}

