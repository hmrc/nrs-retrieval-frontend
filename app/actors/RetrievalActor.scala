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

package actors

import java.util.concurrent.TimeUnit

import javax.inject.Inject
import akka.actor.{Actor, ActorContext, ActorRef}
import akka.pattern.ask

import scala.concurrent.duration._
import akka.util.Timeout
import config.AppConfig
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import connectors.NrsRetrievalConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrievalActor @Inject()(appConfig: AppConfig, pas: ActorService)
                              (implicit nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  val logger = Logger(this.getClass)

  implicit val timeout: Timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  implicit val system: ActorContext = context

  def receive = {
    case SubmitMessage(vaultId, archiveId, headerCarrier) =>
      sender ! submitRetrievalRequest(vaultId, archiveId)(headerCarrier)
    case StatusMessage(vaultId, archiveId) =>
      sender ! pas.eventualPollingActor(vaultId, archiveId)
        .flatMap(aR => aR ? StatusMessage(vaultId, archiveId))
        .recover { case _ => UnknownMessage }
    case IsCompleteMessage(vaultId, archiveId) =>
      sender ! pas.eventualPollingActor(vaultId, archiveId)
        .flatMap(aR => aR ? IsCompleteMessage(vaultId, archiveId))
    case _ =>
      logger.warn(s"An unexpected message has been received")
      sender ! Future(UnknownMessage)
  }


  private def submitRetrievalRequest(vaultId: String, archiveId: String)(implicit headerCarrier: HeaderCarrier) = {
    logger.info(s"Submit retrieval request for vault: $vaultId, archive: $archiveId.")
    nrsRetrievalConnector.submitRetrievalRequest(vaultId, archiveId)
      .flatMap { response =>
        if (response.status == ACCEPTED) {
          pas.pollingActorExists(vaultId, archiveId).flatMap {
            case true => resetPollingActor(vaultId, archiveId)
              Future(PollingMessage)
            case _ => pas.pollingActor(vaultId, archiveId).flatMap(aR => aR ? StatusMessage(vaultId, archiveId))
          }
        } else {
          logger.info(s"Retrieval request submission for vault: $vaultId, archive: $archiveId failed with ${response.status}.")
          Future(UnknownMessage)
        }
      }
  }

  // reset the polling actor if it complete or has failed
  private def resetPollingActor(vaultId: String, archiveId: String) = {
    pas.pollingActor(vaultId, archiveId)
      .flatMap { pA =>
        (pA ? StatusMessage(vaultId, archiveId)).mapTo[ActorMessage].map {
          case CompleteMessage => pA ! RestartMessage
          case FailedMessage(_) => pA ! RestartMessage
        }
      }
  }

}