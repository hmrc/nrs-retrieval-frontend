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

import akka.actor.{Actor, ActorPath, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import config.AppConfig
import connectors.NrsRetrievalConnector
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class CheckStatusActor(pollingActorPath: ActorPath, appConfig: AppConfig)(implicit val nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  implicit def hc: HeaderCarrier = HeaderCarrier(extraHeaders = Seq("X-API-Key" -> appConfig.xApiKey))

  implicit val timeout: Timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  implicit val system: ActorSystem = context.system
  private val logger = Logger(this.getClass)

  def receive: PartialFunction[Any, Unit] = {
    case StatusMessage(vaultId, archiveId) => {
      nrsRetrievalConnector.statusSubmissionBundle(vaultId, archiveId).map { response =>
        response.status match {
          case OK => {
            logger.info(s"Retrieval request complete for vault $vaultId, archive $archiveId")
            pollingActor(vaultId, archiveId).map (aR => aR ! CompleteMessage)
          }
          case NOT_FOUND => logger.info(s"Status check for vault $vaultId, archive $archiveId returned 404")
          case _ => pollingActor(vaultId, archiveId).map(aR => aR ! FailedMessage)
        }
      }
    }
    case _ =>
      logger.warn(s"An unexpected message has been received")
      sender ! UnknownMessage
  }

  private def pollingActor(vaultId: String, archiveId: String)(implicit system: ActorSystem, nrsRetrievalConnector: NrsRetrievalConnector): Future[ActorRef] = {
    try {
      system.actorSelection(pollingActorPath).resolveOne()
    } catch {
      case e: Throwable => Future(system.actorOf(Props(new PollingActor(vaultId, archiveId, appConfig)), s"pollingActor_key_${vaultId}_key_$archiveId"))
    }
  }
}