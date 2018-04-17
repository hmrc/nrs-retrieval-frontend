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

import akka.actor.{Actor, ActorContext, ActorRef, ActorSystem, Cancellable, Props}
import akka.pattern.ask

import scala.concurrent.duration._
import akka.util.Timeout
import config.AppConfig
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import connectors.NrsRetrievalConnector
import org.joda.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

class RetrievalActor @Inject()(appConfig: AppConfig, pas: ActorService)
  (implicit nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  val logger = Logger(this.getClass)

  implicit val timeout: Timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  implicit val system: ActorContext = context

  def receive = {
    case SubmitMessage(vaultId, archiveId, headerCarrier) =>
      submitRetrievalRequest(vaultId, archiveId)(headerCarrier)
    case StatusMessage(vaultId, archiveId) =>
      sender ! pas.pollingActor(vaultId, archiveId).flatMap(aR => aR ? StatusMessage(vaultId, archiveId))
    case _ =>
      logger.warn(s"An unexpected message has been received")
      sender ! Future(UnknownMessage)
  }

  private def submitRetrievalRequest(vaultId: String, archiveId: String)(implicit headerCarrier: HeaderCarrier) = {
    logger.info(s"Submit retrieval request for vault: $vaultId, archive: $archiveId.")
    nrsRetrievalConnector.submitRetrievalRequest(vaultId, archiveId)
      .map(response =>
        if (response.status != OK && response.status != ACCEPTED) {
          logger.info(s"Retrieval request submission for vault: $vaultId, archive: $archiveId failed with ${response.status}.")
        } else {
          sender ! pas.pollingActor(vaultId, archiveId).flatMap(aR => aR ? StatusMessage(vaultId, archiveId))
        }
      )
  }
}