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

package controllers.testonly

import connectors.testonly.TestOnlyNrsRetrievalConnector
import controllers.{ControllerSpec, StrideAuthSettings}
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.mockito.internal.stubbing.answers.Returns
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.html.components.{GovukErrorMessage, GovukHint, GovukInput, GovukLabel}
import uk.gov.hmrc.http.UpstreamErrorResponse
import views.html.components.{Heading1, Paragraph, TextInput}
import views.html.testonly.check_authorisation_page

import scala.concurrent.Future

class CheckAuthorisationControllerSpec extends ControllerSpec {
  private val connector = mock[TestOnlyNrsRetrievalConnector]

  val checkAuthorisationPage = new check_authorisation_page(
    layout,
    new Paragraph,
    new TextInput(new GovukInput(new GovukErrorMessage, new GovukHint, new GovukLabel)),
    new Heading1
  )

  private lazy val controller =
    new CheckAuthorisationController(
      stubMessagesControllerComponents(),
      mockAuthConnector,
      new StrideAuthSettings(),
      error_template,
      connector,
      checkAuthorisationPage
    )

  "checkAuthorisation" should {
    "confirm that the request is authenticated and authorised" when {
      "the user is authenticated and authorised" in {
        doAnswer(new Returns(Future(true))).when(connector).checkAuthorisation()(any())

        contentAsString(controller.checkAuthorisation(getRequest)) should include(messages("test-only.check-authorisation.status.200"))
      }
    }

    "confirm that the request is unauthorised" when {
      "the user is not authenticated" in {
        doAnswer(new Returns(Future failed UpstreamErrorResponse(UNAUTHORIZED.toString, UNAUTHORIZED)))
          .when(connector).checkAuthorisation()(any())

        contentAsString(controller.checkAuthorisation(getRequest)) should include(messages("test-only.check-authorisation.status.401"))
      }
    }

    "confirm that the request is forbidden" when {
      "the user is authenticated but not authorised" in {
        doAnswer(new Returns(Future failed UpstreamErrorResponse(FORBIDDEN.toString, FORBIDDEN)))
          .when(connector).checkAuthorisation()(any())

        contentAsString(controller.checkAuthorisation(getRequest)) should include(messages("test-only.check-authorisation.status.403"))
      }
    }
  }
}
