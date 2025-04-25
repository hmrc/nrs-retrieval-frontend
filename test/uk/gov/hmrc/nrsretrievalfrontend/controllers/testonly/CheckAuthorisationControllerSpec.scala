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

package uk.gov.hmrc.nrsretrievalfrontend.controllers.testonly

import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.mockito.internal.stubbing.answers.Returns
import play.api.test.Helpers.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.nrsretrievalfrontend.connectors.testonly.TestOnlyNrsRetrievalConnector
import uk.gov.hmrc.nrsretrievalfrontend.controllers.ControllerSpec

import scala.concurrent.Future

class CheckAuthorisationControllerSpec extends ControllerSpec:
  private val connector = mock[TestOnlyNrsRetrievalConnector]

  private lazy val controller =
    new CheckAuthorisationController(
      stubMessagesControllerComponents(),
      connector,
      checkAuthorisationPage
    )

  "checkAuthorisation" should {
    "confirm that the request is authenticated and authorised" when {
      "the user is authenticated and authorised" in {
        doAnswer(new Returns(Future(true))).when(connector).checkAuthorisation(using any[HeaderCarrier])

        contentAsString(controller.checkAuthorisation(getRequest)) should include(messages("test-only.check-authorisation.status.200"))
      }
    }

    "confirm that the request is unauthorised" when {
      "the user is not authenticated" in {
        doAnswer(new Returns(Future failed UpstreamErrorResponse(UNAUTHORIZED.toString, UNAUTHORIZED)))
          .when(connector)
          .checkAuthorisation(using any[HeaderCarrier])

        contentAsString(controller.checkAuthorisation(getRequest)) should include(messages("test-only.check-authorisation.status.401"))
      }
    }

    "confirm that the request is forbidden" when {
      "the user is authenticated but not authorised" in {
        doAnswer(new Returns(Future failed UpstreamErrorResponse(FORBIDDEN.toString, FORBIDDEN)))
          .when(connector)
          .checkAuthorisation(using any[HeaderCarrier])

        contentAsString(controller.checkAuthorisation(getRequest)) should include(messages("test-only.check-authorisation.status.403"))
      }
    }
  }
