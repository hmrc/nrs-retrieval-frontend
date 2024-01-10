/*
 * Copyright 2024 HM Revenue & Customs
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

package actions

import actions.requests.AuthenticatedRequest
import config.AppConfig
import controllers.StrideAuthSettings
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.error_template

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton()
class AuthenticatedAction @Inject()(
    val authConnector: AuthConnector,
    val config: Configuration,
    val env: Environment,
    val controllerComponents: MessagesControllerComponents,
    strideAuthSettings: StrideAuthSettings,
    errorPage: error_template
                                   )(implicit val executionContext: ExecutionContext, appConfig: AppConfig)
  extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with AuthorisedFunctions
    with AuthRedirects
    with FrontendBaseController
    with I18nSupport {

  val logger: Logger = Logger(this.getClass.getName)

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val r: Request[A] = request
    if (strideAuthSettings.strideAuthEnabled) {
      logger.info(s"Verify stride authorisation")

      val notAuthorised = "Not authorised"

      authorised(AuthProviders(PrivilegedApplication) and Enrolment("nrs_digital_investigator")).retrieve(credentials and allEnrolments and name) {
        case Some(userCredentials) ~ enrolments ~ Some(userName) =>
          logger.debug(s"authorised, enrolments=$enrolments")

          val userRoles = enrolments.enrolments.map(_.key)
          val userHasOneOrMoreRequiredRoles = strideAuthSettings.strideRoles.intersect(userRoles).nonEmpty

          if (userHasOneOrMoreRequiredRoles) {
            block(AuthenticatedRequest((userName.name.toList ++ userName.lastName.toList).mkString(" "), userCredentials.providerId, request))
          } else {
            Future successful Ok(errorPage(notAuthorised, notAuthorised, s"Insufficient enrolments - ${enrolments.enrolments.map(_.key)}"))
          }
        case None ~ _ ~ _ =>
          Future successful Ok(errorPage(notAuthorised, notAuthorised, s"User credentials not found"))
        case _ ~ _ ~ None =>
          Future successful Ok(errorPage(notAuthorised, notAuthorised, s"User name not found"))
      }.recover {
        case ex: NoActiveSession =>
          logger.info(s"NoActiveSession", ex)
          toStrideLogin(if (appConfig.isLocal) s"http://${request.host}${request.uri}" else s"${request.uri}")
        case ex: InsufficientEnrolments =>
          logger.info(s"error, not authorised: insufficent enrolements", ex)
          Ok(errorPage(notAuthorised, notAuthorised, s"Insufficient enrolments - ${ex.msg}"))
        case ex =>
          logger.warn(s"error, other error", ex)
          Ok(errorPage(notAuthorised, notAuthorised, "Sorry, not authorised"))
      }
    } else {
      logger.debug(s"auth switched off")
      block(AuthenticatedRequest("Auth disabled", "Auth disabled", request))
    }
  }
}
