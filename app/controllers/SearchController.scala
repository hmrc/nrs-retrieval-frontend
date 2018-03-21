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

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import com.google.inject.name.Named
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import actors.SubmitMessage
import config.AppConfig
import connectors.NrsRetrievalConnector
import controllers.SearchController._
import models.{SearchQuery, SearchResult, SearchResults, Search, User}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class SearchController @Inject()(val messagesApi: MessagesApi,
  val nrsRetrievalConnector: NrsRetrievalConnector,
  @Named("retrieval-actor") retrievalActor: ActorRef,
  implicit val appConfig: AppConfig, implicit val system: ActorSystem, implicit val mat: Materializer) extends FrontendController with I18nSupport {

  val logger: Logger = Logger(this.getClass)

  def showSearchPage(searchResults: Option[SearchResults] = None): Action[AnyContent] = Action.async { implicit request =>
    logger.info("Show the search page")
    searchForm.bindFromRequest.fold(
      formWithErrors => {
        logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
        Future.successful(BadRequest(formWithErrors.errors.toString()))
      },
      searchQuery => {
        Future(Ok(views.html.search_page(searchForm.bindFromRequest, searchResults, Some(User(appConfig.userName))))
        )
      }
    )
  }

  def submitSearchPage: Action[AnyContent] = Action.async { implicit request =>
    logger.debug("Execute the search query")
    searchForm.bindFromRequest.fold(
      formWithErrors => {
        logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
        Future.successful(BadRequest(formWithErrors.errors.toString()))
      },
      searchQuery => {
        logger.debug("Execute the search query")
        nrsRetrievalConnector.search(searchQuery.searchText.getOrElse("")).map { nSRs =>
          logger.info("Show the search results page")
          Ok(
            views.html.search_page(
              searchForm.bindFromRequest,
              Some(SearchResults(nSRs.map(nSR => SearchResult.fromNrsSearchResult(nSR)))),
              Some(User(appConfig.userName))
            )
          )
        }
      }
    )
  }

  // todo : stop repeating the query.
  def submitRetrievalRequest(vaultId: Long, archiveId: Long): Action[AnyContent] = Action.async { implicit request =>
    logger.debug("Submit the retrieval request")
    searchForm.bindFromRequest.fold(
      formWithErrors => {
        logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
        Future.successful(BadRequest(formWithErrors.errors.toString()))
      },
      searchQuery => {
        logger.debug("Execute the search query")
        nrsRetrievalConnector.search(searchQuery.searchText.getOrElse("")).map { nSRs =>
          logger.info("Show the search results page")

          SubmitMessage(vaultId, archiveId)

          Ok(
            views.html.search_page(
              searchForm.bindFromRequest,
              Some(models.SearchResults(nSRs.map(nSR => SearchResult.fromNrsSearchResult(nSR)))),
              Some(User(appConfig.userName))
            )
          )
        }
      }
    )
  }

  def poll(vaultId: Long, archiveId: Long): Action[AnyContent] = Action.async { implicit request =>
    Future(Ok(s"Stuff $vaultId, $archiveId"))
  }

}


object SearchController {
  val searchFormOld: Form[SearchQuery] =
    Form(mapping("searchText" -> optional(text))(SearchQuery.apply)(SearchQuery.unapply))

  private val searchQueryMapping = mapping("searchText" -> optional(text))

  private val searchResultMapping = mapping("companyName" -> optional(text),
    "retrievalLink" -> text,
    "filename" -> text,
    "vaultId" -> longNumber,
    "archiveId" -> longNumber,
    "submissionDate" -> date
  )

  private val searchResultsMapping = mapping("searchResults" -> seq[SearchResult](searchResultMapping))

  val searchForm: Form[Search] = Form(
    mapping("query" -> optional(searchQueryMapping),
      "results" -> optional(searchResultsMapping)
    )
    (SearchQuery.apply)(SearchQuery.unapply))
}
