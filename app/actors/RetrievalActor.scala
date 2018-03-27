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

import akka.actor.{Actor, Cancellable, Props}
import akka.pattern.ask

import scala.concurrent.duration._
import akka.util.Timeout
import config.AppConfig
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import connectors.NrsRetrievalConnector
import org.joda.time.Instant
import org.joda.time.Instant
import ActorUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

// kill the polling actor after some interval so that we don't have old stuff hanging around forever
class RetrievalActor @Inject()(appConfig: AppConfig)(implicit nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  val logger = Logger(this.getClass)
  implicit val timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))
  implicit val hc = new HeaderCarrier() // todo : set this up correctly
  implicit val system = context.system

  // get the polling actor for this submission, or create one.
  def receive = {
    case SubmitMessage(vaultId, archiveId) =>
      maybePollingActor(vaultId, archiveId) match {
        case Some(aR) =>
          val statusMessageResponse = aR ? StatusMessage(vaultId, archiveId)
          val c = Await.result(statusMessageResponse, appConfig.futureTimeoutSeconds seconds)
          sender ! c
        case _ =>
          nrsRetrievalConnector.submitRetrievalRequest(vaultId, archiveId) map { response =>
          response.status match {
            case OK =>
              logger.debug("Retrieval request accepted")
              context.actorOf(Props(new PollingActor(vaultId, archiveId, nrsRetrievalConnector)), s"pollingActor_${vaultId}_$archiveId")
              sender ! PollingMessage
            case _ =>
              FailedToStartMessage
          }
        }
      }
    case StatusMessage(vaultId, archiveId) =>
      maybePollingActor(vaultId, archiveId) match {
      case Some(aR) => sender ! Await.result(aR ? StatusMessage, appConfig.futureTimeoutSeconds seconds)
      case _ => sender ! UnknownMessage
    }
    case _ => UnknownMessage
  }
}

// the way to reset this is to stop and restart the actor, need a way to trigger ot
class PollingActor(vaultId: Long, archiveId: Long, implicit val nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  def receive = poll

  implicit val timeout = Timeout(FiniteDuration(3, TimeUnit.SECONDS))

  private val initialDelay = 0.millis
  private val interval = 3000.millis // todo : from config
  private val runTimeMillis = 60000 // todo : from config
  private val stopTime = Instant.now().plus(runTimeMillis)

  val cancellable: Cancellable = context.system.scheduler.schedule(initialDelay, interval) {
    val checkStatusActor = context.actorOf(Props(new CheckStatusActor()))
    if (Instant.now().isBefore(stopTime)) {
      checkStatusActor ! StatusMessage(vaultId, archiveId)
    } else {
      self ! FailedMessage("Timeout")
    }
  }

  def poll: Receive = {
    case StatusMessage => sender ! PollingMessage
    case CompleteMessage =>
      cancellable.cancel()
      context.become(complete)
    case FailedMessage(payload) =>
      cancellable.cancel()
      context.become(failed(payload))
    case _ => UnknownMessage
  }

  def complete: Receive = {
    case StatusMessage =>
      sender ! CompleteMessage
    case _ => "" // log an error
  }

  def failed(payload: String): Receive = {
    case StatusMessage => sender ! FailedMessage(payload)
    case _ => "" // log an error
  }
}

class CheckStatusActor(implicit val nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  implicit val timeout = Timeout(FiniteDuration(30, TimeUnit.SECONDS))

  implicit val hc = new HeaderCarrier() // todo : set this up correctly with headers needed to hit the aws app
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
}

trait ActorMessage

case class SubmitMessage(vaultId: Long, archiveId: Long) extends ActorMessage

case class StatusMessage(vaultId: Long, archiveId: Long) extends ActorMessage

case object PollingMessage extends ActorMessage

case object IncompleteMessage extends ActorMessage

case object CompleteMessage extends ActorMessage

case class FailedMessage(payload: String) extends ActorMessage

case object UnknownMessage extends ActorMessage

case object FailedToStartMessage extends ActorMessage
