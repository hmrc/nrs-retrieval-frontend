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

package uk.gov.hmrc.nrsretrievalfrontend.controllers

import play.api.Logger
import play.api.mvc.*
import uk.gov.hmrc.nrsretrievalfrontend.actions.AuthenticatedAction
import uk.gov.hmrc.nrsretrievalfrontend.config.AppConfig
import uk.gov.hmrc.nrsretrievalfrontend.views.html.start_page

import javax.inject.{Inject, Singleton}

@Singleton
class StartController @Inject() (
  authenticatedAction: AuthenticatedAction,
  controllerComponents: MessagesControllerComponents,
  startPage: start_page
)(using val appConfig: AppConfig)
    extends NRBaseController(controllerComponents):

  val logger: Logger                       = Logger(this.getClass)
  override lazy val parse: PlayBodyParsers = controllerComponents.parsers

  val showStartPage: Action[AnyContent] = authenticatedAction { implicit request =>
    logger.info(s"Show start page")
    Ok(startPage())
  }
