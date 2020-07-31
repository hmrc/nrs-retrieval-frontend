/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import views.html.{error_template, start_page}

@Singleton
class StartController @Inject()(val authConnector: AuthConnector,
                                override val controllerComponents: MessagesControllerComponents,
                                val startPage: start_page,
                                override val errorPage: error_template)
                               (implicit val appConfig: AppConfig) extends FrontendController(controllerComponents)
  with I18nSupport with Stride {

  override val logger: Logger = Logger(this.getClass)
  override val strideRoles: Set[String] = appConfig.nrsStrideRoles
  override lazy val parse: PlayBodyParsers = controllerComponents.parsers

  logger.info(s"appConfig: stride.enabled: ${appConfig.strideAuth}")
  logger.info(s"appConfig: stride.role.name: $strideRoles")
  logger.info(s"appConfig: auth host:port: ${appConfig.authHost}:${appConfig.authPort}")

  def showStartPage: Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"S=how start page")
    authWithStride("Show the start page", { _ =>
      Future.successful(
        Ok(startPage())
      )
    })

  }

}
