/*
 * Copyright 2021 HM Revenue & Customs
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

package actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSystem}
import akka.util.Timeout
import config.AppConfig
import connectors.NrsRetrievalConnector
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class CheckStatusActor(appConfig: AppConfig)(implicit val nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  implicit def hc: HeaderCarrier = HeaderCarrier(extraHeaders = Seq("X-API-Key" -> appConfig.xApiKey))

  implicit val timeout: Timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  implicit val system: ActorSystem = context.system
  private val logger = Logger(this.getClass)

  def receive: PartialFunction[Any, Unit] = {
    case StatusMessage(vaultId, archiveId) => {
      logger.info(s"CheckStatusActor received StatusMessage($vaultId, $archiveId)")
      nrsRetrievalConnector.statusSubmissionBundle(vaultId, archiveId).map { response =>
        response.status match {
          case OK => {
            logger.info(s"Retrieval request complete for vault $vaultId, archive $archiveId")
            context.parent ! CompleteMessage
          }
          case NOT_FOUND => {
            logger.info(s"Status check for vault $vaultId, archive $archiveId returned 404")
            context.parent ! IncompleteMessage
          }
          case _ => {
            logger.info(s"Retrieval request failed for vault $vaultId, archive $archiveId")
            context.parent ! FailedMessage
          }
        }
      }
    }
    case _ =>
      logger.warn(s"An unexpected message has been received")
      sender ! UnknownMessage
  }

}