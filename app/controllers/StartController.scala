/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import akka.actor.ActorSystem
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import config.AppConfig
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class StartController @Inject()(
  val messagesApi: MessagesApi,
  val system: ActorSystem,
  implicit val appConfig: AppConfig,
  val authConnector: AuthConnector
) extends FrontendController with I18nSupport with Stride {

  override val logger: Logger = Logger(this.getClass)
  override val strideRole: String = appConfig.nrsStrideRole

  logger.info(s"appConfig: strideRole: $strideRole")
  logger.info(s"appConfig: strideHost: ${appConfig.strideHost}")

  def showStartPage: Action[AnyContent] = Action.async { implicit request =>
    authWithStride("Show the start page", {
      Future.successful(
        Ok(views.html.start_page())
      )
    })

  }

}
