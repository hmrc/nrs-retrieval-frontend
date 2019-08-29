/*
 * Copyright 2019 HM Revenue & Customs
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

import akka.actor.ActorSystem
import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import controllers.FormMappings._

import scala.concurrent.Future

@Singleton
class SelectorController @Inject()(
                                 val messagesApi: MessagesApi,
                                 val system: ActorSystem,
                                 implicit val appConfig: AppConfig,
                                 val authConnector: AuthConnector
                               ) extends FrontendController with I18nSupport with Stride {

  override val logger: Logger = Logger(this.getClass)
  override val strideRoles: Set[String] = appConfig.nrsStrideRoles

  logger.info(s"appConfig: stride.enabled: ${appConfig.strideAuth}")
  logger.info(s"appConfig: stride.role.name: $strideRoles")
  logger.info(s"appConfig: auth host:port: ${appConfig.authHost}:${appConfig.authPort}")

  def showSelectorPage: Action[AnyContent] = Action.async { implicit request =>
    authWithStride("Show the selector page", { nrUser =>
      Future.successful(
        Ok(views.html.selector_page(selectorForm, Some(nrUser)))
      )
    })
  }

  def submitSelectorPage: Action[AnyContent] = Action.async { implicit request =>
    authWithStride("Submit the selector page", { nrUser =>
      selectorForm.bindFromRequest.fold(
        formWithErrors => {
          logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
          Future.successful(
            Ok(views.html.selector_page(formWithErrors, Some(nrUser)))
          )
        },
        v => {
          Future.successful(
           Redirect(routes.SearchController.showSearchPage(v.notableEventType))
          )
        }
      )
    })
  }

}
