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
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.{AskTimeoutException, ask}
import akka.stream.Materializer
import akka.util.Timeout
import com.google.inject.name.Named
import config.AppConfig
import connectors.NrsRetrievalConnector
import controllers.FormMappings._
import javax.inject.{Inject, Singleton}
import models._
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.error_template

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class SearchController @Inject()(val messagesApi: MessagesApi,
                                 @Named("retrieval-actor") retrievalActor: ActorRef,
                                 implicit val appConfig: AppConfig,
                                 val authConnector: AuthConnector,
                                 val nrsRetrievalConnector: NrsRetrievalConnector,
                                 implicit val system: ActorSystem,
                                 implicit val mat: Materializer,
                                 val searchResultUtils: SearchResultUtils) extends FrontendController with I18nSupport with Stride {

  override val logger: Logger = Logger(this.getClass)
  val strideRole: String = appConfig.nrsStrideRole

  implicit override def hc(implicit rh: RequestHeader): HeaderCarrier = super.hc
    .withExtraHeaders("X-API-Key" -> appConfig.xApiKey)

  implicit val timeout: Timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  def showSearchPage(notableEventType: String): Action[AnyContent] = Action.async { implicit request =>
    authWithStride("Show the search page", { nrUser =>
      searchForm.bindFromRequest.fold(
        formWithErrors => {
          logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
          Future.successful(BadRequest(formWithErrors.errors.toString()))
        },
        _ => {
          Future(Ok(views.html.search_page(searchForm.bindFromRequest, Some(nrUser), notableEventType)))
        }
      )
    })
  }

  def submitSearchPage: Action[AnyContent] = Action.async { implicit request =>
    authWithStride("Submit the search page", { nrUser =>
      searchForm.bindFromRequest.fold(
        formWithErrors => {
          logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
          Future.successful(BadRequest(formWithErrors.errors.toString()))
        },
        search => {
          val sRs: Seq[SearchResult] = search.results.getOrElse(SearchResults(Seq.empty, 0)).results
          doSearch(search, sRs).map { form =>
            Ok(views.html.search_page(form, Some(nrUser), search.query.notableEventType.getOrElse("vat-return")))
          }.recoverWith {case e => Future(Ok(error_template(Messages("error.page.title"), Messages("error.page.heading"), Messages("error.page.message"))))}
        }
      )
    })
  }

  def refresh(vaultName: String, archiveId: String): Action[AnyContent] = Action.async { implicit request =>
    ask(retrievalActor, IsCompleteMessage(vaultName, archiveId)).mapTo[Future[ActorMessage]].flatMap(identity).map {
      case CompleteMessage => Ok(CompletionStatus.complete)
      case FailedMessage => Ok(CompletionStatus.failed)
    } recoverWith {
      case e: AskTimeoutException => Future(Accepted(CompletionStatus.incomplete))
    }
  }

  def doAjaxRetrieve(vaultName: String, archiveId: String): Action[AnyContent] = Action.async { implicit request =>
    ask(retrievalActor, SubmitMessage(vaultName, archiveId, hc)).mapTo[Future[ActorMessage]].flatMap(identity).map {
      case _ => Accepted(CompletionStatus.incomplete)
    } recoverWith {
      case e: AskTimeoutException => Future(Accepted(CompletionStatus.incomplete))
    }
  }

    def download(vaultId: String, archiveId: String): Action[AnyContent] = Action.async { implicit request =>
    nrsRetrievalConnector.getSubmissionBundle(vaultId, archiveId).map { response =>
      Ok(response.bodyAsBytes).withHeaders(mapToSeq(response.allHeaders): _*)
    }.recoverWith {case e => Future(Ok(error_template(Messages("error.page.title"), Messages("error.page.heading"), Messages("error.page.message"))))}
  }

  private def doSearch(search: Search, searchResults: Seq[SearchResult])(implicit hc: HeaderCarrier) = {
    nrsRetrievalConnector.search(search.query)
      .map(fNSR => fNSR.map(nSR => searchResultUtils.fromNrsSearchResult(nSR)))
    .map(s => searchForm.bind(Json.toJson(Search(search.query, Some(SearchResults(s, s.size))))))
  }

  private def doRetrieve(searchResults: Seq[SearchResult], vaultId: String, archiveId: String)(implicit hc: HeaderCarrier) = {
    (for {
      fAM <- ask(retrievalActor, SubmitMessage(vaultId, archiveId, hc)).mapTo[Future[ActorMessage]]
      aM <- fAM
    } yield aM) map {
      case PollingMessage => searchResults map { sR =>
        if (sR.vaultId == vaultId && sR.archiveId == archiveId) {
          sR.copy(retrievalStatus = Some(""))
        } else {
          sR
        }
      }
      case _ => searchResults
    }
  }

  private def getFirstAction(request: Request[AnyContent]): SearchPageAction = {
    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("action").flatMap(_.headOption) match {
      case Some(action) if action == "search" => SearchAction
      case Some(action) if action startsWith "retrieve" =>
        val actionParts: Array[String] = action.split("_key_")
        RetrieveAction(actionParts(1), actionParts(2))
      case _ => UnknownAction
    }
  }

  private def mapToSeq(sourceMap: Map[String, Seq[String]]): Seq[(String, String)] =
    sourceMap.keys.flatMap(k => sourceMap(k).map(v => (k, v))).toSeq

}
