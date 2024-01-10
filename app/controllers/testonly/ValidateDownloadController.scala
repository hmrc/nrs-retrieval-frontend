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

import actions.AuthenticatedAction
import config.AppConfig
import connectors.testonly.TestOnlyNrsRetrievalConnector
import controllers.NRBaseController
import controllers.testonly.FormMappings.validateDownloadForm
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, RequestHeader}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.testonly.validate_download_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ValidateDownloadController @Inject()(
                                            authenticatedAction: AuthenticatedAction,
                                            controllerComponents: MessagesControllerComponents,
                                            connector: TestOnlyNrsRetrievalConnector,
                                            validateDownloadPage: validate_download_page)
                                          (implicit val appConfig: AppConfig, executionContext: ExecutionContext)
  extends NRBaseController(controllerComponents) {

  val logger: Logger = Logger(this.getClass)

  implicit override def hc(implicit rh: RequestHeader): HeaderCarrier =
    super.hc.withExtraHeaders("X-API-Key" -> appConfig.xApiKey)

  val showValidateDownload: Action[AnyContent] = authenticatedAction.async { implicit request =>
    Future successful Ok(validateDownloadPage(validateDownloadForm))
  }

  val submitValidateDownload: Action[AnyContent] = authenticatedAction.async { implicit request =>
        validateDownloadForm.bindFromRequest().fold(
          _ => Future.successful(BadRequest),
          validateDownloadRequest => {
            connector.validateDownload(validateDownloadRequest.vaultName, validateDownloadRequest.archiveId).map{ response =>
              Ok(validateDownloadPage(validateDownloadForm.fill(validateDownloadRequest), Some(response)))}
          }
        )
  }
}

