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

  def noParameters(): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"No parameters provided so redirecting to start page")
    Future(Redirect(routes.StartController.showStartPage()))
  }

  def showSearchPage(notableEventType: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Show the search page for notable event $notableEventType")
    authWithStride("Show the search page", { nrUser =>
      Future(Ok(views.html.search_page(searchForm.fill(SearchQuery(None, None, notableEventType)), Some(nrUser), None)))
    })
  }

  def submitSearchPage(notableEventType: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Submit the search page for notable event $notableEventType")
    authWithStride("Submit the search page", { user =>
      searchForm.bindFromRequest.fold(
        formWithErrors => {
          logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
          Future.successful(BadRequest(formWithErrors.errors.toString()))
        },
        search => {
          logger.info(s"Do search for submitted search query ${search.searchText}")
          doSearch(search, user).map { results =>
            logger.info(s"Form $results")
            Ok(views.html.search_page(searchForm.bindFromRequest, Some(user), Some(results)))
          }.recoverWith { case e =>
            logger.info(s"SubmitSearchPage $e")
            Future(Ok(error_template(Messages("error.page.title"), Messages("error.page.heading"), Messages("error.page.message"))))
          }
        }
      )
    })
  }

  private def doSearch(search: SearchQuery, user: AuthorisedUser)(implicit hc: HeaderCarrier) = {
    nrsRetrievalConnector.search(search, user)
      .map(fNSR => fNSR.map(nSR => searchResultUtils.fromNrsSearchResult(nSR)))
  }

  def refresh(vaultName: String, archiveId: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Refresh the result $vaultName, $archiveId")
    ask(retrievalActor, IsCompleteMessage(vaultName, archiveId)).mapTo[Future[ActorMessage]].flatMap(identity).map {
      case CompleteMessage =>
        logger.info(s"Retrieval completed for $vaultName, $archiveId")
        Ok(CompletionStatus.complete)
      case FailedMessage =>
        logger.info(s"Retrieval failed for $vaultName, $archiveId")
        Ok(CompletionStatus.failed)
    } recoverWith {
      case e: AskTimeoutException =>
        logger.info(s"Retrieval is still in progress for $vaultName, $archiveId")
        Future(Accepted(CompletionStatus.incomplete))
    }
  }

  def doAjaxRetrieve(vaultName: String, archiveId: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Request retrieval for $vaultName, $archiveId")
    authWithStride("Download", { user =>
      ask(retrievalActor, SubmitMessage(vaultName, archiveId, hc, user)).mapTo[Future[ActorMessage]].flatMap(identity).map {
        case _ =>
          logger.info(s"Retrieval accepted for $vaultName, $archiveId")
          Accepted(CompletionStatus.incomplete)
      } recoverWith {
        case e: AskTimeoutException =>
          logger.info(s"Retrieval is still in progress for $vaultName, $archiveId")
          Future(Accepted(CompletionStatus.incomplete))
      }
    })
  }

  def download(vaultName: String, archiveId: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Request dowload of $vaultName, $archiveId")
    authWithStride("Download", { user =>
      nrsRetrievalConnector.getSubmissionBundle(vaultName, archiveId, user).map { response =>
        logger.info(s"Dowload of $vaultName, $archiveId")
        Ok(response.bodyAsBytes).withHeaders(mapToSeq(response.allHeaders): _*)
      }.recoverWith { case e =>
        logger.info(s"Dowload of $vaultName, $archiveId failed with $e")
        Future(Ok(error_template(Messages("error.page.title"), Messages("error.page.heading"), Messages("error.page.message")))) }
    })
  }

  private def mapToSeq(sourceMap: Map[String, Seq[String]]): Seq[(String, String)] =
    sourceMap.keys.flatMap(k => sourceMap(k).map(v => (k, v))).toSeq

}
