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

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.concurrent.duration._
import akka.util.Timeout
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import connectors.NrsRetrievalConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await

class CheckStatusActor(implicit val nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  implicit val timeout = Timeout(FiniteDuration(30, TimeUnit.SECONDS))

  implicit val hc = new HeaderCarrier()
  implicit val system = context.system
  val logger = Logger(this.getClass)

  def receive = {
    case StatusMessage(vaultId, archiveId) => {
      nrsRetrievalConnector.statusSubmissionBundle(vaultId, archiveId).map { response =>
        response.status match {
          case OK => {
            logger.info(s"Retrieval request complete for vault $vaultId, archive $archiveId")
            pollingActor(vaultId, archiveId) ! CompleteMessage
          }
          case NOT_FOUND => logger.info(s"Status check for vault $vaultId, archive $archiveId returned 404")
          case UNAUTHORIZED => pollingActor(vaultId, archiveId) ! FailedMessage(UNAUTHORIZED.toString)
        }
      }
    }
    case _ => UnknownMessage
  }

  private def pollingActor(vaultId: Long, archiveId: Long)(implicit system: ActorSystem, nrsRetrievalConnector: NrsRetrievalConnector): ActorRef = {
    try {
      Await.result(system.actorSelection(s"akka://application/user/retrieval-actor/pollingActor_${vaultId}_$archiveId").resolveOne(), 5 seconds)
    } catch {
      case e: Throwable => system.actorOf(Props(new PollingActor(vaultId, archiveId)), s"pollingActor_${vaultId}_$archiveId")
    }
  }
}