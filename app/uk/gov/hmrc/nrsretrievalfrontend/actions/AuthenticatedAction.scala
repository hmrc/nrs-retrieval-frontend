/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.nrsretrievalfrontend.actions

import play.api.i18n.I18nSupport
import play.api.mvc.*
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.nrsretrievalfrontend.actions.requests.AuthenticatedRequest
import uk.gov.hmrc.nrsretrievalfrontend.config.{AppConfig, AuthRedirects}
import uk.gov.hmrc.nrsretrievalfrontend.views.html.error_template
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton()
class AuthenticatedAction @Inject()(
                                     val authConnector: AuthConnector,
                                     val config: Configuration,
                                     val env: Environment,
                                     val controllerComponents: MessagesControllerComponents,
                                     errorPage: error_template
                                   )(using val executionContext: ExecutionContext, appConfig: AppConfig)
  extends ActionBuilder[AuthenticatedRequest, AnyContent], AuthorisedFunctions , FrontendBaseController, AuthRedirects, I18nSupport:

  val logger: Logger = Logger(this.getClass.getName)
  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    given r: Request[A] = request

    logger.info(s"Verify stride authorisation")

    val notAuthorised = "Not authorised"

    authorised(
      AuthProviders(PrivilegedApplication) and
        (Enrolment("nrs_digital_investigator") or Enrolment("nrs digital investigator"))
    ).retrieve(credentials) {
      case Some(userCredentials) =>
        block(AuthenticatedRequest(userCredentials.providerId, request))
      case None =>
        Future successful Forbidden(errorPage(notAuthorised, notAuthorised, s"User credentials not found"))
    }.recover {
      case ex: NoActiveSession =>
        logger.warn(s"NoActiveSession", ex)
        toStrideLogin(if (appConfig.isLocal) s"http://${request.host}${request.uri}" else s"${request.uri}")
      case ex: InsufficientEnrolments =>
        logger.warn(s"error, not authorised: insufficent enrolements", ex)
        Forbidden(errorPage(notAuthorised, notAuthorised, s"Insufficient enrolments - ${ex.msg}"))
    }
  }

