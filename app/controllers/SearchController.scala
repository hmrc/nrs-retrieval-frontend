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

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.Materializer
import com.google.inject.name.Named
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import actors._
import akka.util.Timeout
import config.AppConfig
import connectors.NrsRetrievalConnector
import controllers.SearchController._
import models._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.FiniteDuration

@Singleton
class SearchController @Inject()(val messagesApi: MessagesApi,
                                 @Named("retrieval-actor") retrievalActor: ActorRef,
                                 implicit val nrsRetrievalConnector: NrsRetrievalConnector,
                                 implicit val appConfig: AppConfig,
                                 implicit val system: ActorSystem,
                                 implicit val mat: Materializer) extends FrontendController with I18nSupport {

  val logger: Logger = Logger(this.getClass)
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  def showSearchPage: Action[AnyContent] = Action.async { implicit request =>
    logger.info("Show the search page")
    searchForm.bindFromRequest.fold(
      formWithErrors => {
        logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
        Future.successful(BadRequest(formWithErrors.errors.toString()))
      },
      search => {
        Future(Ok(views.html.search_page(searchForm.bindFromRequest, Some(User(appConfig.userName))))
        )
      }
    )
  }

  // get and display results
  def submitSearchPage: Action[AnyContent] = Action.async { implicit request =>
    logger.debug("Submit the search page")
    searchForm.bindFromRequest.fold(
      formWithErrors => {
        logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
        Future.successful(BadRequest(formWithErrors.errors.toString()))
      },
      search => {
        getFormData(request, search).map{form =>
          Ok(views.html.search_page(form, Some(User(appConfig.userName)))
          )
        }
      }
    )
  }

  private def getFormData(request: Request[AnyContent], search: Search) = {
    getFirstAction(request) match {
      case RefreshAction => doRefresh(search)
      case RetrieveAction(vaultId, archiveId) => doRetrieve(search, vaultId, archiveId)
      case DownloadAction(vaultId, archiveId) => doDownload(search, vaultId, archiveId)
      case SearchAction => doSearch(search)
      case _ => doShow(search)
    }
  }

  private def doSearch(search: Search) = {
    search.query.searchText.map { query =>
      nrsRetrievalConnector.search(query)
        .map(fNSR => fNSR.map(nSR => SearchResult.fromNrsSearchResult(nSR)))
    }.getOrElse(Future(Seq.empty)).map { sRs =>
      searchForm.bind(Json.toJson(Search(search.query, Some(SearchResults(sRs, sRs.size)))))
    }
  }

  private def doRefresh(search: Search) = {
    search.query.searchText.map { query =>
      nrsRetrievalConnector.search(query).map { fNSR =>
            fNSR.map { nSR =>
              setRetrievalStatus(SearchResult.fromNrsSearchResult(nSR))
            }
        }
    }.getOrElse(Future(Seq.empty)).map { sRs =>
      searchForm.bind(Json.toJson(Search(search.query, Some(SearchResults(sRs, sRs.size)))))
    }
  }

  private def doShow(search: Search) = {
    Future(searchForm.bind(Json.toJson(Search(search.query, search.results))))
  }

  private def doRetrieve(search: Search, vaultId: Long, archiveId: Long) = {
    ask(retrievalActor, SubmitMessage(vaultId, archiveId)).mapTo[Future[ActorMessage]]
    doRefresh(search)
  }

  private def doDownload(search: Search, vaultId: Long, archiveId: Long) = {
    nrsRetrievalConnector.getSubmissionBundle(vaultId, archiveId) map {response =>
      response.body.getBytes()

    }
    doRefresh(search)
  }

  private def setRetrievalStatus (searchResult: SearchResult): SearchResult = {

    val s = for {
      fAM <- ask(retrievalActor, StatusMessage(searchResult.vaultId, searchResult.archiveId))
        .mapTo[Future[ActorMessage]]
      aM <- fAM
    } yield aM

    Await.result(s, appConfig.futureTimeoutSeconds seconds) match {
      case PollingMessage => searchResult.copy(retrievalInProgress = true, retrievalSucceeded = false, retrievalFailed = false)
      case CompleteMessage => searchResult.copy(retrievalInProgress = false, retrievalSucceeded = true, retrievalFailed = false)
      case FailedMessage(payload) => searchResult.copy(retrievalInProgress = false, retrievalSucceeded = false, retrievalFailed = true)
      case _ => searchResult
    }
  }

  private def getFirstAction(request: Request[AnyContent]): SearchPageAction = {
    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("action").flatMap(_.headOption) match {
      case Some(action) if action == "search" => SearchAction
      case Some(action) if action == "refresh" => RefreshAction
      case Some(action) if action startsWith "retrieve" =>
        val actionParts: Array[String] = action.split("_")
        RetrieveAction(actionParts(1).toLong, actionParts(2).toLong)
      case Some(action) if action startsWith "download" =>
        val actionParts: Array[String] = action.split("_")
        DownloadAction(actionParts(1).toLong, actionParts(2).toLong)
      case _ => UnknownAction
    }
  }


}

trait SearchPageAction
case object UnknownAction extends SearchPageAction
case object SearchAction extends SearchPageAction
case object RefreshAction extends SearchPageAction
case class DownloadAction (vaultId: Long, archiveId: Long) extends SearchPageAction
case class RetrieveAction (vaultId: Long, archiveId: Long) extends SearchPageAction

object SearchController {
  private val searchQueryMapping = mapping(
    "searchText" -> optional(text)
  )(SearchQuery.apply)(SearchQuery.unapply)

  private val searchResultMapping = mapping(
    "retrievalLink" -> text,
    "fileName" -> text,
    "vaultId" -> longNumber,
    "archiveId" -> longNumber,
    "submissionDateEpochMilli" -> longNumber,
    "retrievalInProgress" -> boolean,
    "retrievalSucceeded" -> boolean,
    "retrievalFailed" -> boolean
  )(SearchResult.apply)(SearchResult.unapply)

  private val searchResultsMapping = mapping(
    "results" -> seq[SearchResult](searchResultMapping),
    "resultCount" -> number
  )(SearchResults.apply)(SearchResults.unapply)


  val searchForm: Form[Search] = Form(
    mapping("query" -> searchQueryMapping,
      "results" -> optional(searchResultsMapping)
    )
    (Search.apply)(Search.unapply))

  val searchQueryForm: Form[SearchQuery] = Form(searchQueryMapping)

  val searchResultsForm: Form[SearchResults] = Form(searchResultsMapping)

}