/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.nrsretrievalfrontend.actions

import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.NotFound
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.nrsretrievalfrontend.actions.requests.{AuthenticatedRequest, NotableEventRequest}
import uk.gov.hmrc.nrsretrievalfrontend.config.AppConfig
import uk.gov.hmrc.nrsretrievalfrontend.views.html.error_template

import scala.concurrent.{ExecutionContext, Future}

class NotableEventRefiner(
                           val messagesApi: MessagesApi,
                           errorPage: error_template
                         )(notableEventType: String)(using val executionContext: ExecutionContext, appConfig: AppConfig)
  extends ActionRefiner[AuthenticatedRequest, NotableEventRequest], I18nSupport:

  val logger = Logger(this.getClass.getName)

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, NotableEventRequest[A]]] = Future {
    implicit val r: AuthenticatedRequest[A] = request
    val optNotableEvent = appConfig.notableEvents.get(notableEventType)

    optNotableEvent.flatMap(NotableEventRequest.apply(_, request)).toRight {
      logger.warn(s"notable event ${notableEventType} not found")
      NotFound(errorPage("Not found", "Not found", s"Notable Event Type not found"))
    }
  }

