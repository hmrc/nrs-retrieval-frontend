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

import org.apache.pekko.util.Timeout
import uk.gov.hmrc.mdc.Mdc
import play.api.Logger
import play.api.mvc.*
import uk.gov.hmrc.nrsretrievalfrontend.actions.requests.NotableEventRequest
import uk.gov.hmrc.nrsretrievalfrontend.actions.{AuthenticatedAction, NotableEventRefiner}
import uk.gov.hmrc.nrsretrievalfrontend.config.AppConfig
import uk.gov.hmrc.nrsretrievalfrontend.connectors.NrsRetrievalConnector
import uk.gov.hmrc.nrsretrievalfrontend.controllers.FormMappings.*
import uk.gov.hmrc.nrsretrievalfrontend.models.{CompletionStatus, SearchQueries, SearchResultUtils}
import uk.gov.hmrc.nrsretrievalfrontend.views.html.*

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class MetaSearchController @Inject() (
  authenticatedAction: AuthenticatedAction,
  notableEventRefinerFunction: String => NotableEventRefiner,
  nrsRetrievalConnector: NrsRetrievalConnector,
  searchResultUtils: SearchResultUtils,
  controllerComponents: MessagesControllerComponents,
  searchPage: metasearch_page,
  errorPage: error_template
)(using val appConfig: AppConfig, executionContext: ExecutionContext)
    extends NRBaseController(controllerComponents):

  val logger: Logger                       = Logger(this.getClass)
  override lazy val parse: PlayBodyParsers = controllerComponents.parsers

  given timeout: Timeout = Timeout(
    FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS)
  )

  val noParameters: Action[AnyContent] = Action.async { request =>
    logger.info(
      s"No parameters provided so redirecting to start page on request $request"
    )
    Future(Redirect(routes.StartController.showStartPage))
  }

  private val action: String => ActionBuilder[NotableEventRequest, AnyContent] = notableEventType =>
    Mdc.putMdc(Map("notable_event" -> notableEventType))
    authenticatedAction.andThen(notableEventRefinerFunction(notableEventType))

  def showSearchPage(notableEventType: String): Action[AnyContent] =
    action(notableEventType) { implicit request =>
      logger.info(s"Show the search page for notable event $notableEventType")
      Ok(searchPage(form, None, getEstimatedRetrievalTime(notableEventType)))
    }

  def submitSearchPage(notableEventType: String): Action[AnyContent] =
    action(notableEventType).async { implicit request =>
      logger.info(s"Submit the search page for notable event $notableEventType")
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            logger.info(s"Form has errors ${formWithErrors.errors.toString()}")
            Future(
              BadRequest(
                searchPage(
                  formWithErrors,
                  None,
                  getEstimatedRetrievalTime(notableEventType)
                )
              )
            )
          ,
          search =>
            doSearch(search)
              .map { results =>
                logger.info(s"Form $results")
                Ok(
                  searchPage(
                    form.fill(search),
                    Some(results),
                    getEstimatedRetrievalTime(notableEventType)
                  )
                )
              }
              .recover { case e =>
                logger.info(s"SubmitSearchPage $e")
                InternalServerError(
                  errorPage(
                    request.messages.messages("error.page.title"),
                    request.messages.messages("error.page.heading"),
                    request.messages.messages("error.page.message")
                  )
                )
              }
        )
    }

  private def doSearch(
    search: SearchQueries
  )(using request: NotableEventRequest[?]) =
    logger.info(
      s"doing search for ${request.notableEvent} with submitted search query ${search.queries}"
    )

    nrsRetrievalConnector
      .metaSearch(request.notableEvent.name, search.queries)
      .map(fNSR => fNSR.map(nSR => searchResultUtils.fromNrsSearchResult(nSR)))

  def refresh(vaultName: String, archiveId: String): Action[AnyContent] =
    Action.async { request =>
      given Request[AnyContent] = request
      logger.info(
        s"Refresh the result $vaultName, $archiveId on request $request"
      )
      nrsRetrievalConnector
        .statusSubmissionBundle(vaultName, archiveId)
        .map(_.status)
        .map {
          case OK        =>
            logger.info(s"Retrieval completed for $vaultName, $archiveId")
            Ok(CompletionStatus.complete)
          case NOT_FOUND =>
            logger.info(s"Retrieval in progress for $vaultName, $archiveId")
            Accepted(CompletionStatus.incomplete)
          case _         =>
            logger.info(s"Retrieval failed for $vaultName, $archiveId")
            Ok(CompletionStatus.failed)
        }
        .recover { case NonFatal(e) =>
          logger.info(s"Retrieval status failed with ${e.getMessage}", e)
          Accepted(CompletionStatus.incomplete)
        }
    }

  private def getEstimatedRetrievalTime(
    notableEventType: String
  ): FiniteDuration =
    appConfig.notableEvents
      .get(notableEventType)
      .fold(5.minutes)(_.estimatedRetrievalTime)
