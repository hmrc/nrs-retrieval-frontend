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

package controllers

import config.AppConfig
import controllers.FormMappings._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{error_template, selector_page}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectorController @Inject()(val authConnector: AuthConnector,
                                   override val controllerComponents: MessagesControllerComponents,
                                   override val strideAuthSettings: StrideAuthSettings,
                                   val selectorPage: selector_page,
                                   override val errorPage: error_template)
                                  (implicit val appConfig: AppConfig, executionContext: ExecutionContext)
  extends FrontendController(controllerComponents) with I18nSupport with Stride {

  override val logger: Logger = Logger(this.getClass)
  override lazy val parse: PlayBodyParsers = controllerComponents.parsers

  logger.info(s"appConfig: auth host:port: ${appConfig.authHost}:${appConfig.authPort}")

  val showSelectorPage: Action[AnyContent] = Action.async { implicit request =>
    authWithStride("Show the selector page", { nrUser =>
      Future.successful(
        Ok(selectorPage(selectorForm, Some(nrUser)))
      )
    })
  }

  val submitSelectorPage: Action[AnyContent] = Action.async { implicit request =>
    authWithStride("Submit the selector page", { nrUser =>
      selectorForm.bindFromRequest.fold(
        formWithErrors => {
          logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
          Future.successful(
            Ok(selectorPage(formWithErrors, Some(nrUser)))
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
