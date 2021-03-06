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

import akka.actor.{Actor, Cancellable, Props}
import akka.pattern.ask
import akka.util.Timeout
import config.AppConfig
import connectors.NrsRetrievalConnector
import org.joda.time.Instant
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class PollingActor (vaultId: String, archiveId: String, appConfig: AppConfig)
  (implicit val nrsRetrievalConnector: NrsRetrievalConnector) extends Actor {

  def receive: Receive = poll

  implicit val timeout: Timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  implicit def hc: HeaderCarrier = HeaderCarrier(extraHeaders = Seq("X-API-Key" -> appConfig.xApiKey))

  private val logger = Logger(this.getClass)

  private val checkStatusActor = context.actorOf(Props(new CheckStatusActor(appConfig)))

  private var cancellable: Cancellable = startStatusCheck

  private def startStatusCheck = {
    val stopTime = Instant.now().plus(appConfig.runTimeMillis)
    context.system.scheduler.scheduleWithFixedDelay(appConfig.interval, appConfig.interval)(() => {
      if (Instant.now().isAfter(stopTime)) {
        self ! FailedMessage
      }
    })
  }

  private def stopStatusCheck = if (!cancellable.isCancelled) cancellable.cancel()

  def poll: Receive = {
    case StatusMessage(_, _) =>
      logger.info(s"Retrieval request for vault: $vaultId, archive: $archiveId is in progress.")
      sender ! PollingMessage
    case CompleteMessage =>
      logger.info(s"Retrieval request for vault: $vaultId, archive: $archiveId has successfully completed.")
      stopStatusCheck
      context.become(complete)
    case FailedMessage =>
      logger.info(s"Retrieval request for vault: $vaultId, archive: $archiveId has failed.")
      stopStatusCheck
      context.become(failed)
    case RestartMessage =>
      logger.info(s"Restart polling.")
      sender ! PollingMessage
    case IsCompleteMessage(_, _) => checkStatusActor ? StatusMessage(vaultId, archiveId)
    case msg => logger.warn(s"An unexpected message $msg has been received by an actor handling vault: $vaultId, archive: $archiveId")
  }

  def complete: Receive = {
    case StatusMessage(_, _) => sender ! CompleteMessage
    case IsCompleteMessage(_, _) => sender ! CompleteMessage
    case RestartMessage =>
      cancellable = startStatusCheck
      context.become(poll)
    case msg => logger.warn(s"An unexpected message $msg has been received by an actor handling vault: $vaultId, archive: $archiveId")
  }

  def failed: Receive = {
    case StatusMessage(_, _) => sender ! FailedMessage
    case IsCompleteMessage(_, _) => sender ! FailedMessage
    case RestartMessage =>
      cancellable = startStatusCheck
      context.become(poll)
    case msg => logger.warn(s"An unexpected message $msg has been received by an actor handling vault: $vaultId, archive: $archiveId")
  }
}