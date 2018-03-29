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

import akka.actor.{ActorContext, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import com.google.inject.Inject
import config.AppConfig
import connectors.NrsRetrievalConnector

import scala.concurrent.Await
import scala.concurrent.duration._

trait PollingActorService {

  def startPollingActor(vaultId: String, archiveId: String)(implicit context: ActorContext, nrsRetrievalConnector: NrsRetrievalConnector): ActorRef = ???

  def maybePollingActor(vaultId: String, archiveId: String)(implicit context: ActorContext, nrsRetrievalConnector: NrsRetrievalConnector): Option[ActorRef] = ???
 }

class PollingActorServiceImpl @Inject()(appConfig: AppConfig) extends PollingActorService {

  implicit val timeout = Timeout(FiniteDuration(5, TimeUnit.SECONDS))

  override def startPollingActor(vaultId: String, archiveId: String)(implicit context: ActorContext, nrsRetrievalConnector: NrsRetrievalConnector): ActorRef =
    context.actorOf(Props(new PollingActor(vaultId, archiveId, appConfig)), s"pollingActor_${vaultId}_$archiveId")

  override def maybePollingActor(vaultId: String, archiveId: String)(implicit context: ActorContext, nrsRetrievalConnector: NrsRetrievalConnector): Option[ActorRef] = {
      try {
        // todo : consider a non-blocking strategy to get the polling actor ref from the path
        Some(Await.result(context.actorSelection(s"akka://application/user/retrieval-actor/pollingActor_${vaultId}_$archiveId").resolveOne(), 5 seconds))
      } catch {
        case e: Throwable => None
      }
    }

}
