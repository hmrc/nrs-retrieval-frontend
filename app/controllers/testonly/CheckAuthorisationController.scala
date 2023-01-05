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

import config.AppConfig
import connectors.testonly.TestOnlyNrsRetrievalConnector
import controllers.{Stride, StrideAuthSettings}
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.error_template
import views.html.testonly.check_authorisation_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CheckAuthorisationController @Inject()(override val controllerComponents: MessagesControllerComponents,
                                             override val authConnector: AuthConnector,
                                             override val strideAuthSettings: StrideAuthSettings,
                                             override val errorPage: error_template,
                                             connector: TestOnlyNrsRetrievalConnector,
                                             checkAuthorisationPage: check_authorisation_page)
                                            (implicit val appConfig: AppConfig, executionContext: ExecutionContext)
  extends FrontendController(controllerComponents) with Stride {

  override val logger: Logger = Logger(this.getClass)

  val checkAuthorisation: Action[AnyContent] = Action.async { implicit request =>
      connector.checkAuthorisation().map { _ =>
        Ok(checkAuthorisationPage("test-only.check-authorisation.status.200"))
      }.recover {
        case e: UpstreamErrorResponse =>
          Ok(checkAuthorisationPage(s"test-only.check-authorisation.status.${e.statusCode}"))
      }
  }
}
