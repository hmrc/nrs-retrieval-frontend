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

import akka.actor.{ActorContext, ActorNotFound, ActorRef, Props}
import akka.util.Timeout
import com.google.inject.Inject
import config.AppConfig
import connectors.NrsRetrievalConnector
import play.api.Logger

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait ActorService {

  val logger = Logger(this.getClass)

  def startPollingActor(vaultId: String, archiveId: String)
    (implicit context: ActorContext, nrsRetrievalConnector: NrsRetrievalConnector): ActorRef = ???

  def eventualPollingActor(vaultId: String, archiveId: String)
                          (implicit context: ActorContext, nrsRetrievalConnector: NrsRetrievalConnector): Future[ActorRef] = ???

  def pollingActor(vaultId: String, archiveId: String)
                  (implicit context: ActorContext, nrsRetrievalConnector: NrsRetrievalConnector): Future[ActorRef] = ???
}

class ActorServiceImpl @Inject()(appConfig: AppConfig) extends ActorService {

  implicit val timeout = Timeout(FiniteDuration(5, TimeUnit.SECONDS))

  override def startPollingActor(vaultId: String, archiveId: String)(implicit context: ActorContext, nrsRetrievalConnector: NrsRetrievalConnector): ActorRef =
    context.actorOf(Props(new PollingActor(vaultId, archiveId, appConfig)), s"pollingActor_key_${vaultId}_key_$archiveId")

  override def eventualPollingActor(vaultId: String, archiveId: String)
                                   (implicit context: ActorContext, nrsRetrievalConnector: NrsRetrievalConnector): Future[ActorRef] =
      context.actorSelection(s"akka://application/user/retrieval-actor/pollingActor_key_${vaultId}_key_$archiveId").resolveOne()

  override def pollingActor(vaultId: String, archiveId: String)
                           (implicit context: ActorContext, nrsRetrievalConnector: NrsRetrievalConnector): Future[ActorRef] =
    eventualPollingActor(vaultId, archiveId).recover {case _: ActorNotFound => startPollingActor(vaultId, archiveId)}

}
