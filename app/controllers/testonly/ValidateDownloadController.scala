/*
 * Copyright 2021 HM Revenue & Customs
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

import config.{AppConfig, ViewConfig}
import connectors.testonly.TestOnlyNrsRetrievalConnector
import controllers.Stride
import controllers.testonly.FormMappings.validateDownloadForm
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, RequestHeader}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.error_template
import views.html.testonly.validate_download_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ValidateDownloadController @Inject()(override val controllerComponents: MessagesControllerComponents,
                                           override val authConnector: AuthConnector,
                                           override val errorPage: error_template,
                                           connector: TestOnlyNrsRetrievalConnector,
                                           validateDownloadPage: validate_download_page)
                                          (implicit val appConfig: AppConfig, viewConfig: ViewConfig)
  extends FrontendController(controllerComponents) with Stride {

  override val logger: Logger = Logger(this.getClass)
  override val strideRoles: Set[String] = appConfig.nrsStrideRoles

  private val authAction = "Validate download"

  implicit override def hc(implicit rh: RequestHeader): HeaderCarrier = super.hc
    .withExtraHeaders("X-API-Key" -> appConfig.xApiKey)

  val showValidateDownload: Action[AnyContent] = Action.async { implicit request =>
    authWithStride(
      authAction, { _ =>
        Future successful Ok(validateDownloadPage(validateDownloadForm))
      }
    )
  }

  val submitValidateDownload: Action[AnyContent] = Action.async { implicit request =>
    authWithStride(
      authAction, { user =>
        validateDownloadForm.bindFromRequest.fold(
          _ => Future.successful(BadRequest),
          validateDownloadRequest => {
            connector.validateDownload(validateDownloadRequest.vaultName, validateDownloadRequest.archiveId, user).map{ response =>
              Ok(validateDownloadPage(validateDownloadForm.bindFromRequest, Some(response)))}
          }
        )
      }
    )
  }
}

