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

package uk.gov.hmrc.nrsretrievalfrontend.controllers

import javax.inject.{Inject, Singleton}

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.nrsretrievalfrontend.config.AppConfig
import uk.gov.hmrc.nrsretrievalfrontend.model.SearchQuery
import uk.gov.hmrc.nrsretrievalfrontend.views
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class SearchController @Inject()(val messagesApi: MessagesApi,
                                 implicit val appConfig: AppConfig) extends FrontendController with I18nSupport {

  val showSearchPage: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(views.html.search(selectTaxYearForm)))
  }

  val submitSearchPage: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(views.html.search(selectTaxYearForm)))
  }

  val selectTaxYearForm: Form[SearchQuery] = {
    Form(mapping(
      "searchText" -> text
    )(SearchQuery.apply)(SearchQuery.unapply))
  }

}
