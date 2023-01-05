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

import akka.util.Timeout
import config.AppConfig
import connectors.NrsRetrievalConnector
import controllers.FormMappings._
import models._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{error_template, search_page}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.control.NonFatal

@Singleton
class SearchController @Inject()(
                                  val authConnector: AuthConnector,
                                  val nrsRetrievalConnector: NrsRetrievalConnector,
                                 val searchResultUtils: SearchResultUtils,
                                 override val controllerComponents: MessagesControllerComponents,
                                 override val strideAuthSettings: StrideAuthSettings,
                                 val searchPage: search_page,
                                 override val errorPage: error_template)
                                (implicit val appConfig: AppConfig, executionContext: ExecutionContext)
  extends FrontendController(controllerComponents) with I18nSupport with Stride {

  override val logger: Logger = Logger(this.getClass)
  override lazy val parse: PlayBodyParsers = controllerComponents.parsers

  implicit val timeout: Timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  val noParameters: Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"No parameters provided so redirecting to start page on request $request")
    Future(Redirect(routes.StartController.showStartPage))
  }

  def showSearchPage(notableEventType: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Show the search page for notable event $notableEventType")
    authWithStride("Show the search page", { nrUser =>
      Future(Ok(searchPage(searchForm.fill(SearchQuery(None, None, notableEventType)), Some(nrUser), None, getEstimatedRetrievalTime(notableEventType))))
    })
  }

  def submitSearchPage(notableEventType: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Submit the search page for notable event $notableEventType")
    authWithStride("Submit the search page", { user =>
      searchForm.bindFromRequest.fold(
        formWithErrors => {
          logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
          Future(BadRequest(formWithErrors.errors.toString()))
        },
        search => {
          doSearch(search, user).map { results =>
            logger.info(s"Form $results")
            Ok(searchPage(searchForm.bindFromRequest, Some(user), Some(results), getEstimatedRetrievalTime(notableEventType)))
          }.recover {
            case e =>
              logger.info(s"SubmitSearchPage $e")
              Ok(errorPage(request.messages("error.page.title"), request.messages("error.page.heading"), request.messages("error.page.message")))
          }
        }
      )
    })
  }

  private def doSearch(search: SearchQuery, user: AuthorisedUser)(implicit hc: HeaderCarrier) = {
    val crossKeySearch = appConfig.notableEvents.get(search.notableEventType).fold(false)(_.crossKeySearch)

    logger.info(s"Do search for submitted search query ${search.searchText(crossKeySearch)}")

    nrsRetrievalConnector.search(search, user, crossKeySearch)
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

  def refreshAjax(vaultName: String, archiveId: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Refresh the result $vaultName, $archiveId on ajax call")

    nrsRetrievalConnector.statusSubmissionBundle(vaultName, archiveId).map { response =>
      response.status match {
        case OK =>
          logger.info(s"Retrieval request complete for vault $vaultName, archive $archiveId")
          Ok(CompletionStatus.complete)
        case NOT_FOUND =>
          logger.info(s"Status check for vault $vaultName, archive $archiveId returned 404")
          Accepted(CompletionStatus.incomplete)
        case _ =>
          logger.info(s"Retrieval request failed for vault $vaultName, archive $archiveId")
          Ok(CompletionStatus.failed)
      }
    } recoverWith {
      case e: Exception =>
        logger.warn(s"Retrieval is still in progress for $vaultName, $archiveId, $e")
        Future(Accepted(CompletionStatus.incomplete))
    }
  }

  def doAjaxRetrieve(vaultName: String, archiveId: String): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Request retrieval for $vaultName, $archiveId")
    authWithStride("Download", { user =>
      nrsRetrievalConnector.submitRetrievalRequest(vaultName, archiveId, user).map { _ =>
        logger.info(s"Retrieval accepted for $vaultName, $archiveId")
        Accepted(CompletionStatus.incomplete)
      }
    })
  }

  def download(vaultName: String, archiveId: String): Action[AnyContent] = Action.async { implicit request =>
    val messagePrefix = s"Request download of $vaultName, $archiveId"

    logger.info(messagePrefix)

    authWithStride("Download", { user =>
      nrsRetrievalConnector.getSubmissionBundle(vaultName, archiveId, user).map { response =>
        val bytes = response.bodyAsBytes

        // log response size rather than the content as this might contain sensitive information
        logger.info(s"$messagePrefix received status: [${response.status}] headers: [${response.headers}] and ${bytes.size} bytes from upstream.")

        Ok(bytes).withHeaders(mapToSeq(response.headers): _*)
      }.recoverWith { case e =>
        logger.error(s"$messagePrefix failed with $e")

        Future(Ok(errorPage(request.messages("error.page.title"), request.messages("error.page.heading"), request.messages("error.page.message"))))
      }
    })
  }

  private def mapToSeq(sourceMap: Map[String, Seq[String]]): Seq[(String, String)] =
    sourceMap.keys.flatMap(k => sourceMap(k).map(v => (k, v))).toSeq

  private def getEstimatedRetrievalTime(notableEventType: String): FiniteDuration =
    appConfig.notableEvents.get(notableEventType).fold(5.minutes)(_.estimatedRetrievalTime)
}
