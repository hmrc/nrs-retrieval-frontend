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

import FormMappings._
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.nrsretrievalfrontend.actions.AuthenticatedAction
import uk.gov.hmrc.nrsretrievalfrontend.config.AppConfig
import uk.gov.hmrc.nrsretrievalfrontend.views.html.selector_page

import javax.inject.{Inject, Singleton}

@Singleton
class SelectorController @Inject() (
  authenticatedAction: AuthenticatedAction,
  controllerComponents: MessagesControllerComponents,
  selectorPage: selector_page
)(using val appConfig: AppConfig)
    extends NRBaseController(controllerComponents):

  val logger: Logger                       = Logger(this.getClass)
  override lazy val parse: PlayBodyParsers = controllerComponents.parsers

  val showSelectorPage: Action[AnyContent] = authenticatedAction { implicit request =>
    Ok(selectorPage(selectorForm))
  }

  val submitSelectorPage: Action[AnyContent] = authenticatedAction { implicit request =>
    selectorForm
      .bindFromRequest()
      .fold(
        formWithErrors =>
          logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
          Ok(selectorPage(formWithErrors))
        ,
        v => Redirect(routes.SearchController.showSearchPage(v.notableEventType))
      )
  }
