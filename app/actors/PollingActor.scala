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

import akka.actor.{Actor, Cancellable, Props}

import scala.concurrent.duration._
import akka.util.Timeout
import config.AppConfig
import play.api.Logger
import connectors.NrsRetrievalConnector
import org.joda.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global

class PollingActor (vaultId: String, archiveId: String, appConfig: AppConfig)
  (implicit val nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  def receive = poll

  implicit val timeout: Timeout = Timeout(FiniteDuration(3, TimeUnit.SECONDS))
  private val initialDelay = 0.millis
  private var stopTime = Instant.now().plus(appConfig.runTimeMillis)

  private val logger = Logger(this.getClass)

  val cancellable: Cancellable = context.system.scheduler.schedule(initialDelay, appConfig.interval) {
    val checkStatusActor = context.actorOf(Props(new CheckStatusActor(self.path, appConfig)))
    if (Instant.now().isBefore(stopTime)) {
      checkStatusActor ! StatusMessage(vaultId, archiveId)
    } else {
      self ! FailedMessage("Timeout")
    }
  }

  def poll: Receive = {
    case StatusMessage(v, a) if v == vaultId && a == archiveId =>
      logger.info(s"Retrieval request for vault: $vaultId, archive: $archiveId is in progress.")
      sender ! PollingMessage
    case StatusMessage(v, a) =>
      logger.warn(s"Status message request for vault: $v, archive: $a has been sent to an actor handling vault: $vaultId, archive: $archiveId")
      sender ! UnknownMessage
    case CompleteMessage =>
      logger.info(s"Retrieval request for vault: $vaultId, archive: $archiveId has successfully completed.")
      cancellable.cancel()
      context.become(complete)
    case FailedMessage(payload) =>
      logger.info(s"Retrieval request for vault: $vaultId, archive: $archiveId has failed.")
      cancellable.cancel()
      context.become(failed(payload))
    case RestartMessage =>
      sender ! PollingMessage
    case _ =>
      logger.warn(s"An unexpected message has been received by an actor handling vault: $vaultId, archive: $archiveId")
      sender ! UnknownMessage
  }

  def complete: Receive = {
    case StatusMessage(v, a) if v == vaultId && a == archiveId =>
      sender ! CompleteMessage
    case StatusMessage(v, a) =>
      logger.warn(s"Status message request for vault: $v, archive: $a has been sent to an actor handling vault: $vaultId, archive: $archiveId")
      sender ! UnknownMessage
    case RestartMessage =>
      stopTime = Instant.now().plus(appConfig.runTimeMillis)
      context.become(poll)
    case _ =>
      logger.warn(s"An unexpected message has been received by an actor handling vault: $vaultId, archive: $archiveId")
      sender ! UnknownMessage
  }

  def failed(payload: String): Receive = {
    case StatusMessage(v, a) if v == vaultId && a == archiveId =>
      sender ! FailedMessage(payload)
    case StatusMessage(v, a) =>
      logger.warn(s"Status message request for vault: $v, archive: $a has been sent to an actor handling vault: $vaultId, archive: $archiveId")
      sender ! UnknownMessage
    case RestartMessage =>
      stopTime = Instant.now().plus(appConfig.runTimeMillis)
      context.become(poll)
    case _ =>
      logger.warn(s"An unexpected message has been received by an actor handling vault: $vaultId, archive: $archiveId")
      sender ! UnknownMessage
  }
}