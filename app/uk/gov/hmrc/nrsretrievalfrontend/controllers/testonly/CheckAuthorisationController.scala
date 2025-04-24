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

import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.nrsretrievalfrontend.connectors.testonly.TestOnlyNrsRetrievalConnector
import uk.gov.hmrc.nrsretrievalfrontend.controllers.NRBaseController
import uk.gov.hmrc.nrsretrievalfrontend.views.html.testonly.check_authorisation_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CheckAuthorisationController @Inject() (
  controllerComponents: MessagesControllerComponents,
  connector: TestOnlyNrsRetrievalConnector,
  checkAuthorisationPage: check_authorisation_page
)(using executionContext: ExecutionContext)
    extends NRBaseController(controllerComponents):

  val logger: Logger = Logger(this.getClass)

  val checkAuthorisation: Action[AnyContent] = Action.async { implicit request =>
    connector.checkAuthorisation
      .map { _ =>
        Ok(checkAuthorisationPage("test-only.check-authorisation.status.200"))
      }
      .recover { case e: UpstreamErrorResponse =>
        InternalServerError(
          checkAuthorisationPage(
            s"test-only.check-authorisation.status.${e.statusCode}"
          )
        )
      }
  }
