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

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.pattern.ask

import scala.concurrent.duration._
import akka.util.Timeout
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import connectors.NrsRetrievalConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

// kill the polling actor after some interval so that we don't have old stuff hanging around forever
class RetrievalActor @Inject()(implicit val nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  val logger = Logger(this.getClass)
  implicit val timeout = Timeout(FiniteDuration(1, TimeUnit.SECONDS))
  implicit val hc = new HeaderCarrier() // todo : set this up correctly

  // get the polling actor for this submission, or create one.
  def receive = {
    case SubmitMessage(vaultId, archiveId) =>
      context.actorSelection(s"akka://application/user/retrieval-actor/pollingActor_${vaultId}_$archiveId").resolveOne().onComplete {
        case Success(aR) => aR ! StatusMessage(vaultId, archiveId)
        case Failure(e) =>
          nrsRetrievalConnector.submitRetrievalRequest(vaultId, archiveId) map { response =>
            response.status match {
              case OK =>
                logger.debug("Retrieval request accepted")
                val pollingActor = context.actorOf(Props(new PollingActor(vaultId, archiveId, nrsRetrievalConnector)), s"pollingActor_${vaultId}_$archiveId")
                pollingActor ! StartMessage(vaultId, archiveId)
                PollingMessage // handle a failure to start, say if the infrastructure is down, and stop the actor?
              case _ => FailedToStartMessage
            }
          }
      }
    case _ => UnknownMessage
  }
}

// the way to reset this is to stop and restart the actor, need a way to trigger ot
class PollingActor(vaultId: Long, archiveId: Long, implicit val nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  def receive = poll

  implicit val timeout = Timeout(FiniteDuration(1, TimeUnit.SECONDS))

  private val initialDelay = 0.millis
  private val interval = 3000.millis // todo : from config
  private val runTimeMillis = 600000 // todo : from config
  private val stopTime = Instant.now().plusMillis(runTimeMillis)

  private val cancellable = context.system.scheduler.schedule(initialDelay, interval) {
    val checkStatusActor = context.actorOf(Props(new CheckStatusActor()))
    if (Instant.now().isBefore(stopTime)) {
       checkStatusActor ! StatusMessage(vaultId, archiveId)
    } else {
      self ! FailedMessage("Timeout")
    }
  }

  def poll: Receive = {
    case StatusMessage => sender() ! PollingMessage
    case CompleteMessage =>
      cancellable.cancel()
      context.become(complete)
    case FailedMessage(payload) =>
      cancellable.cancel()
      context.become(failed(payload))
    case _ => UnknownMessage
  }

  // todo : do something to route back to the webpage.
  // think about how to push this out the controller
  def complete: Receive = {
    case StatusMessage => sender() ! CompleteMessage
    case _ => sender() ! UnknownMessage
  }

  def failed(payload: String): Receive = {
    case StatusMessage => sender() ! FailedMessage(payload)
    case _ => sender() ! UnknownMessage
  }
}

class CheckStatusActor(implicit val nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  implicit val timeout = Timeout(FiniteDuration(1, TimeUnit.SECONDS))

  implicit val hc = new HeaderCarrier() // todo : set this up correctly with headers needed to hit the aws app
  val logger = Logger(this.getClass)

  def receive = {
    case StatusMessage(vaultId, archiveId) => {
      nrsRetrievalConnector.statusSubmissionBundle(vaultId, archiveId).map { response =>
        response.status match {
          case OK => {
            logger.info(s"Retrieval request complete for vault $vaultId, archive $archiveId")
            context.actorSelection(s"akka://application/user/retrieval-actor/pollingActor_${vaultId}_$archiveId").resolveOne().map{ aR =>
              aR ! CompleteMessage
            }
          }
          case NOT_FOUND => logger.info(s"Status check for vault $vaultId, archive $archiveId returned 404")
          case UNAUTHORIZED => {
            context.actorSelection(s"akka://application/user/retrieval-actor/pollingActor_${vaultId}_$archiveId").resolveOne().map{ aR =>
              aR ! FailedMessage(UNAUTHORIZED.toString)
            }
          }
        }
      }
    }
    case _ => UnknownMessage
  }
}

trait ActorMessage

case class SubmitMessage(vaultId: Long, archiveId: Long) extends ActorMessage

case class StartMessage(vaultId: Long, archiveId: Long) extends ActorMessage

case class StatusMessage(vaultId: Long, archiveId: Long) extends ActorMessage

case object PollingMessage extends ActorMessage

case object IncompleteMessage extends ActorMessage

case object CompleteMessage extends ActorMessage

case class FailedMessage(payload: String) extends ActorMessage

case object UnknownMessage extends ActorMessage

case object FailedToStartMessage extends ActorMessage
