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

class RetrievalActor @Inject()(appConfig: AppConfig, pas: PollingActorService)
  (implicit nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  val logger = Logger(this.getClass)
  implicit val timeout: Timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))
  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val system: ActorContext = context

  // get the polling actor for this submission, or create one.
  def receive = {
    case SubmitMessage(vaultId, archiveId) =>
      sender ! (pas.maybePollingActor(vaultId, archiveId) match {
        case Some(aR) => ask(aR, StatusMessage(vaultId, archiveId)).mapTo[ActorMessage]
        case _ =>
          nrsRetrievalConnector.submitRetrievalRequest(vaultId, archiveId) map { response =>
            response.status match {
              case OK =>
                logger.info("Retrieval request accepted")
                pas.startPollingActor(vaultId, archiveId)
                StartedMessage
              case _ =>
                logger.info("Retrieval request failed")
                FailedToStartMessage
            }
          }
      })
    case StatusMessage(vaultId, archiveId) =>
      sender ! (pas.maybePollingActor(vaultId, archiveId) match {
        case Some(aR) =>
          ask(aR, StatusMessage(vaultId, archiveId)).mapTo[ActorMessage]
        case _ =>
          logger.warn(s"An unexpected message has been received")
          Future(UnknownMessage)
      })
    case _ =>
      logger.warn(s"An unexpected message has been received")
      sender ! Future(UnknownMessage)
  }
}