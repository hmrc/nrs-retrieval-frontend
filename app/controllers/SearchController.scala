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

package controllers

import actions.requests.NotableEventRequest
import actions.{AuthenticatedAction, NotableEventRefiner}
import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.{ByteString, Timeout}
import config.AppConfig
import connectors.NrsRetrievalConnector
import controllers.FormMappings._
import models._
import play.api.Logger
import play.api.http.HttpEntity
import play.api.mvc._
import views.html.{error_template, search_page}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class SearchController @Inject()(
    authenticatedAction: AuthenticatedAction,
    notableEventRefinerFunction: String => NotableEventRefiner,
                                  val nrsRetrievalConnector: NrsRetrievalConnector,
                                 val searchResultUtils: SearchResultUtils,
                                 controllerComponents: MessagesControllerComponents,
                                 val strideAuthSettings: StrideAuthSettings,
                                 val searchPage: search_page,
                                 val errorPage: error_template)
                                (implicit val appConfig: AppConfig, executionContext: ExecutionContext)
  extends NRBaseController(controllerComponents) {

  val logger: Logger = Logger(this.getClass)
  override lazy val parse: PlayBodyParsers = controllerComponents.parsers

  implicit val timeout: Timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  val noParameters: Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"No parameters provided so redirecting to start page on request $request")
    Future(Redirect(routes.StartController.showStartPage))
  }

  private val action: String => ActionBuilder[NotableEventRequest, AnyContent] = {
    notableEventType =>
      authenticatedAction.andThen(notableEventRefinerFunction(notableEventType))
  }

  def showSearchPage(notableEventType: String): Action[AnyContent] = action(notableEventType) { implicit request =>
    logger.info(s"Show the search page for notable event $notableEventType")
    println(form.fill(SearchQueries(List())).data)
    Ok(searchPage(form, None, getEstimatedRetrievalTime(notableEventType)))
  }

  def submitSearchPage(notableEventType: String): Action[AnyContent] = action(notableEventType).async { implicit request =>
    logger.info(s"Submit the search page for notable event $notableEventType")
    form.bindFromRequest().fold(
        formWithErrors => {
          logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
          Future(BadRequest(searchPage(formWithErrors, None, getEstimatedRetrievalTime(notableEventType))))
        },
        search => {
          doSearch(search).map { results =>
            logger.info(s"Form $results")
            Ok(searchPage(form.fill(search), Some(results), getEstimatedRetrievalTime(notableEventType)))
          }.recover {
            case e =>
              logger.info(s"SubmitSearchPage $e")
              Ok(errorPage(request.messages.messages("error.page.title"), request.messages.messages("error.page.heading"), request.messages.messages("error.page.message")))
          }
        }
      )
  }

  private def doSearch(search: SearchQueries)(implicit request: NotableEventRequest[_]) = {
    val crossKeySearch = appConfig.notableEvents.get(request.notableEvent.name).fold(false)(_.crossKeySearch)

//    logger.info(s"Do search for submitted search query ${search.searchText(request.notableEvent.name, crossKeySearch)}")

    nrsRetrievalConnector.search(request.notableEvent.name, search.queries, crossKeySearch)
      .map(fNSR => fNSR.map(nSR => searchResultUtils.fromNrsSearchResult(nSR)))
  }

  def refresh(vaultName: String, archiveId: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Refresh the result $vaultName, $archiveId on request $request")
    nrsRetrievalConnector.statusSubmissionBundle(vaultName, archiveId).map(_.status).map {
      case OK =>
        logger.info(s"Retrieval completed for $vaultName, $archiveId")
        Ok(CompletionStatus.complete)
      case NOT_FOUND =>
        logger.info(s"Retrieval in progress for $vaultName, $archiveId")
        Accepted(CompletionStatus.incomplete)
      case _ =>
        logger.info(s"Retrieval failed for $vaultName, $archiveId")
        Ok(CompletionStatus.failed)
    }.recover {
      case NonFatal(e) =>
        logger.info(s"Retrieval status failed with ${e.getMessage}", e)
        Accepted(CompletionStatus.incomplete)
    }
  }

  def doAjaxRetrieve(vaultName: String, archiveId: String): Action[AnyContent] = authenticatedAction.async { implicit request =>
    logger.info(s"Request retrieval for $vaultName, $archiveId")
      nrsRetrievalConnector.submitRetrievalRequest(vaultName, archiveId).map { _ =>
        logger.info(s"Retrieval accepted for $vaultName, $archiveId")
        Accepted(CompletionStatus.incomplete)
      }
  }

  def download(vaultName: String, archiveId: String): Action[AnyContent] = authenticatedAction.async { implicit request =>
    val messagePrefix = s"Request download of $vaultName, $archiveId"

    logger.info(messagePrefix)

      nrsRetrievalConnector.getSubmissionBundle(vaultName, archiveId).map { response =>
        val bytes: ByteString = response.bodyAsBytes

        val source: Source[ByteString, NotUsed] = Source.single(bytes)
        // log response size rather than the content as this might contain sensitive information
        logger.info(s"$messagePrefix received status: [${response.status}] headers: [${response.headers}] and ${bytes.size} bytes from upstream.")

        val ignoredHeaders = List("content-type", "content-length")

        new Result(
          header = ResponseHeader(
            status = OK,
            headers = mapToSeq(response.headers.filter(header => ignoredHeaders.contains(header._1))).toMap
          ),
          body = HttpEntity.Streamed(source, Some(bytes.length.toLong), Some(response.contentType))
        )
//        Ok(bytes).withHeaders(mapToSeq(response.headers): _*)
      }.recoverWith { case e =>
        logger.error(s"$messagePrefix failed with $e")

        Future(Ok(errorPage(request.messages.messages("error.page.title"), request.messages.messages("error.page.heading"), request.messages.messages("error.page.message"))))
      }
  }

  private def mapToSeq(sourceMap: Map[String, scala.collection.Seq[String]]): Seq[(String, String)] =
    sourceMap.keys.flatMap(k => sourceMap(k).map(v => (k, v))).toSeq

  private def getEstimatedRetrievalTime(notableEventType: String): FiniteDuration =
    appConfig.notableEvents.get(notableEventType).fold(5.minutes)(_.estimatedRetrievalTime)
}
