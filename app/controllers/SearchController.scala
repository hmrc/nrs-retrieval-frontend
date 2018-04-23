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

import actors._

import javax.inject.{Inject, Singleton}
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.google.inject.name.Named
import config.AppConfig
import connectors.NrsRetrievalConnector
import controllers.SearchController._
import javax.inject.{Inject, Singleton}
import models._
import play.api.Logger
import play.api.data.Form
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import actors._
import akka.util.Timeout
import config.{AppConfig, Auditable}
import connectors.NrsRetrievalConnector
import controllers.SearchController._
import models._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

@Singleton
class SearchController @Inject()(
  val messagesApi: MessagesApi,
  @Named("retrieval-actor") retrievalActor: ActorRef,
  implicit val appConfig: AppConfig,
  // val authConn: AuthConnector, // TODO stride mdtp
  implicit val nrsRetrievalConnector: NrsRetrievalConnector,
  implicit val system: ActorSystem,
  implicit val mat: Materializer) extends FrontendController with I18nSupport with Stride {

  val logger: Logger = Logger(this.getClass)
  val strideRole = appConfig.nrsStrideRole

  implicit override def hc(implicit rh: RequestHeader): HeaderCarrier = super.hc
    .withExtraHeaders("X-API-Key" -> appConfig.xApiKey)

  implicit val timeout: Timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  // override def authConnector: AuthConnector = authConn // TODO stride mdtp

  def showSearchPage: Action[AnyContent] = Action.async { implicit request =>
    authWithStride("Show the search page", {
      searchForm.bindFromRequest.fold(
        formWithErrors => {
          logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
          Future.successful(BadRequest(formWithErrors.errors.toString()))
        },
        search => {
          val out = Ok(views.html.search_page(searchForm.bindFromRequest, Some(NRUser(appConfig.userName))))
          Future(Ok(views.html.search_page(searchForm.bindFromRequest, Some(NRUser(appConfig.userName)))))
        }
      )
    })
  }

  def submitSearchPage: Action[AnyContent] = Action.async { implicit request =>
    authWithStride("Submit the search page", {
      searchForm.bindFromRequest.fold(
        formWithErrors => {
          logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
          Future.successful(BadRequest(formWithErrors.errors.toString()))
        },
        search => {
          getFormData(request, search).map { form =>
            Ok(views.html.search_page(form, Some(NRUser(appConfig.userName))))
          }
        }
      )
    })
  }

  def download(vaultId: String, archiveId: String): Action[AnyContent] = Action.async { implicit request =>
    nrsRetrievalConnector.getSubmissionBundle(vaultId, archiveId).map { response =>
      Ok(response.bodyAsBytes).withHeaders(mapToSeq(response.allHeaders):_*)
    }
  }

  def reset(vaultId: String, archiveId: String): Action[AnyContent] = Action.async { implicit request =>
    retrievalActor ! RestartMessage
    Future(Accepted(""))
  }

  private def getFormData(request: Request[AnyContent], search: Search)(implicit hc: HeaderCarrier) = {
    getFirstAction(request) match {
      case RefreshAction => doRefresh(search)
      case RetrieveAction(vaultId, archiveId) => doRetrieve(search, vaultId, archiveId)
      case DownloadAction(vaultId, archiveId) => doDownload(search, vaultId, archiveId)
      case SearchAction => doSearch(search)
      case _ => doShow(search)
    }
  }

  private def doSearch(search: Search)(implicit hc: HeaderCarrier) = {
    search.query.searchText.map { query =>
      nrsRetrievalConnector.search(query)
        .map(fNSR => fNSR.map(nSR => SearchResult.fromNrsSearchResult(nSR)))
    }.getOrElse(Future(Seq.empty)).map { sRs =>
      searchForm.bind(Json.toJson(Search(search.query, Some(SearchResults(sRs, sRs.size)))))
    }
  }

  private def doRefresh(search: Search)(implicit hc: HeaderCarrier) = {
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

  private def doRetrieve(search: Search, vaultId: String, archiveId: String)(implicit hc: HeaderCarrier) = {
    (retrievalActor ? SubmitMessage(vaultId, archiveId, hc)).mapTo[Future[ActorMessage]]

    doRefresh(search)
  }

  private def doDownload(search: Search, vaultId: String, archiveId: String)(implicit hc: HeaderCarrier) = {
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
        val actionParts: Array[String] = action.split("_key_")
        RetrieveAction(actionParts(1), actionParts(2))
      case Some(action) if action startsWith "download" =>
        val actionParts: Array[String] = action.split("_key_")
        DownloadAction(actionParts(1), actionParts(2))
      case _ => UnknownAction
    }
  }

  private def mapToSeq(sourceMap: Map[String, Seq[String]]): Seq[(String, String)] =
    sourceMap.keys.flatMap(k => sourceMap(k).map(v => (k, v))).toSeq

  private def rewriteResponse (response: HttpResponse) = {
    val headers: Seq[(String, String)] = mapToSeq(response.allHeaders)
    response.status match {
      case 200 => Ok(response.body).withHeaders(headers:_*)
      case 404 => NotFound(response.body)
      case _ => Ok(response.body)
    }
  }

  private def rewriteResponseBytes (response: HttpResponse) = {
    val headers: Seq[(String, String)] = mapToSeq(response.allHeaders)
    response.status match {
      case 200 => Ok(response.body.getBytes).withHeaders(headers:_*)
      case 404 => NotFound(response.body.getBytes)
      case _ => Ok(response.body.getBytes)
    }
  }

}

trait SearchPageAction
case object UnknownAction extends SearchPageAction
case object SearchAction extends SearchPageAction
case object RefreshAction extends SearchPageAction
case class DownloadAction (vaultId: String, archiveId: String) extends SearchPageAction
case class RetrieveAction (vaultId: String, archiveId: String) extends SearchPageAction

object SearchController {
  private val searchQueryMapping = mapping(
    "searchText" -> optional(text)
  )(SearchQuery.apply)(SearchQuery.unapply)

  private val searchResultMapping = mapping(
    "retrievalLink" -> text,
    "fileName" -> text,
    "vaultId" -> text,
    "archiveId" -> text,
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