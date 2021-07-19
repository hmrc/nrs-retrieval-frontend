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

package controllers

import config.{AppConfig, ViewConfig}
import models.AuthorisedUser
import play.api.{Configuration, Environment, Logger}
import play.api.i18n.I18nSupport
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, Retrievals, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.error_template

import scala.concurrent.{ExecutionContext, Future}

trait Stride extends AuthorisedFunctions with AuthRedirects with FrontendBaseController with I18nSupport {

  val strideRoles: Set[String]
  val logger: Logger
  val appConfig: AppConfig
  val config: Configuration = appConfig.runModeConfiguration
  val env: Environment = appConfig.environment
  val authConnector: AuthConnector
  val errorPage: error_template

  private def convertRolesSetToPredicate(enrolments: Set[String]): Predicate = {
    enrolments.map(Enrolment.apply).reduce(
      (enrolment: Predicate, accumulatedPredicate: Predicate) => enrolment or accumulatedPredicate
    )
  }

  private def strideAuthorised[B](f: Credentials ~ Enrolments ~ Name => Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] = {
    authorised(
      convertRolesSetToPredicate(strideRoles).and(AuthProviders(PrivilegedApplication))
    ).retrieve(
      Retrievals.credentials.and(Retrievals.authorisedEnrolments).and(Retrievals.name)
    ) {
      f
    }
  }

  def authWithStride(actionName: String, f: AuthorisedUser => Future[Result])(
    implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext, conf: AppConfig, viewConfig: ViewConfig): Future[Result] = {
    if (appConfig.strideAuth) {
      logger.info(s"Verify stride authorisation")
      strideAuthorised {
        case credentials ~ enrolments ~ name =>
          logger.debug(s"$actionName - authorised, enrolments=$enrolments")
          f(AuthorisedUser((name.name.toList ++ name.lastName.toList).mkString(" "), credentials.providerId))
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
        case ex:InsufficientEnrolments =>
          logger.info(s"$actionName - error, not authorised", ex)
          Ok(errorPage("Not authorised", "Not authorised", s"Insufficient enrolments - ${ex.msg}"))
        case ex =>
          logger.warn(s"$actionName - error, other error", ex)
          Ok(errorPage("Not authorised", "Not authorised", "Sorry, not authorised"))
      }
    } else {
      logger.debug(s"$actionName - auth switched off")
      f(AuthorisedUser("Auth disabled", "Auth disabled"))
    }
  }
}
