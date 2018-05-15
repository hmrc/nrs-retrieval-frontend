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
import play.api.inject.Injector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class PollingActor (vaultId: String, archiveId: String, appConfig: AppConfig)
  (implicit val nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  def receive = poll

  implicit def hc:  HeaderCarrier = HeaderCarrier(extraHeaders = Seq("X-API-Key" -> appConfig.xApiKey))

  implicit val timeout: Timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  private val initialDelay = 0.millis
  private var stopTime = Instant.now().plus(appConfig.runTimeMillis)

  private val logger = Logger(this.getClass)

  val cancellable: Cancellable = context.system.scheduler.schedule(initialDelay, appConfig.interval) {
    val checkStatusActor = context.actorOf(Props(new CheckStatusActor(self.path, appConfig)))
    if (Instant.now().isBefore(stopTime)) {
      checkStatusActor ! StatusMessage(vaultId, archiveId)
    } else {
      self ! FailedMessage
    }
  }

  // do not respond to an IsCompleteMessage
  def poll: Receive = {
    case StatusMessage(_, _) =>
      logger.info(s"Retrieval request for vault: $vaultId, archive: $archiveId is in progress.")
      sender ! PollingMessage
    case CompleteMessage =>
      logger.info(s"Retrieval request for vault: $vaultId, archive: $archiveId has successfully completed.")
      cancellable.cancel()
      context.become(complete)
    case FailedMessage =>
      logger.info(s"Retrieval request for vault: $vaultId, archive: $archiveId has failed.")
      cancellable.cancel()
      context.become(failed)
    case RestartMessage =>
      sender ! PollingMessage
    case _ => logger.warn(s"An unexpected message has been received by an actor handling vault: $vaultId, archive: $archiveId")
  }

  def complete: Receive = {
    case StatusMessage(_, _) => sender ! CompleteMessage
    case IsCompleteMessage(_, _) => sender ! CompleteMessage
    case RestartMessage =>
      stopTime = Instant.now().plus(appConfig.runTimeMillis)
      context.become(poll)
    case _ => logger.warn(s"An unexpected message has been received by an actor handling vault: $vaultId, archive: $archiveId")
  }

  def failed: Receive = {
    case StatusMessage(_, _) => sender ! FailedMessage
    case IsCompleteMessage(_, _) => sender ! FailedMessage
    case RestartMessage =>
      stopTime = Instant.now().plus(appConfig.runTimeMillis)
      context.become(poll)
    case _ => logger.warn(s"An unexpected message has been received by an actor handling vault: $vaultId, archive: $archiveId")
  }
}