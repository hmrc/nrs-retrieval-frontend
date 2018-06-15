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

import config.AppConfig
import models.NRUser
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Controller, Request, Result}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, Retrievals, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import views.html.error_template

import scala.concurrent.{ExecutionContext, Future}

trait Stride extends AuthorisedFunctions with AuthRedirects with Controller with I18nSupport {

  val strideRole: String
  val logger: Logger
  val appConfig: AppConfig
  val config = appConfig.runModeConfiguration
  val env = appConfig.environment
  val authConnector: AuthConnector

  private def strideAuthorised[B](f: Credentials ~ Enrolments ~ Name => Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] = {
    authorised(
      Enrolment(strideRole).and(AuthProviders(PrivilegedApplication))
    ).retrieve(
      Retrievals.credentials.and(Retrievals.authorisedEnrolments).and(Retrievals.name)
    ) {
      f
    }
  }

  def authWithStride(actionName: String, f: NRUser => Future[Result])(
    implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext, conf: AppConfig): Future[Result] = {

    if (appConfig.strideAuth) {
      strideAuthorised {
        case credentials ~ enrolments ~ name =>
          logger.debug(s"$actionName - authorised, enrolments=$enrolments")
          f(NRUser((name.name.toList ++ name.lastName.toList).mkString(" ")))
      }.recover {
        case _: NoActiveSession =>
          logger.debug(s"$actionName - NoActiveSession")
          toStrideLogin(
            if (appConfig.isLocal) {
              s"http://${request.host}${request.uri}"
            }
            else {
              s"${request.uri}"
            })
        case ex@InsufficientEnrolments(`strideRole`) =>
          logger.info(s"$actionName - error, not authorised", ex)
          Ok(error_template("Not authorised", "Not authorised", s"Insufficient enrolments - ${ex.msg}"))
        case ex =>
          logger.warn(s"$actionName - error, other error", ex)
          Ok(error_template("Not authorised", "Not authorised", "Sorry, not authorised"))
      }
    } else {
      logger.debug(s"$actionName - auth switched off")
      f(NRUser(appConfig.userName))
    }

  }

}
