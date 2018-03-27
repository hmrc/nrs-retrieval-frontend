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

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout
import connectors.NrsRetrievalConnector

import scala.concurrent.Await
import scala.concurrent.duration._

object ActorUtils {

  implicit val timeout = Timeout(FiniteDuration(5, TimeUnit.SECONDS))

  def pollingActor(vaultId: Long, archiveId: Long)(implicit system: ActorSystem, nrsRetrievalConnector: NrsRetrievalConnector): ActorRef = {
    try {
      Await.result(system.actorSelection(s"akka://application/user/retrieval-actor/pollingActor_${vaultId}_$archiveId").resolveOne(), 5 seconds)
    } catch {
      case e: Throwable => system.actorOf(Props(new PollingActor(vaultId, archiveId, nrsRetrievalConnector)), s"pollingActor_${vaultId}_$archiveId")
    }
  }

    def maybePollingActor(vaultId: Long, archiveId: Long)(implicit system: ActorSystem, nrsRetrievalConnector: NrsRetrievalConnector): Option[ActorRef] = {
      try {
        Some(Await.result(system.actorSelection(s"akka://application/user/retrieval-actor/pollingActor_${vaultId}_$archiveId").resolveOne(), 5 seconds))
      } catch {
        case e: Throwable => None
      }
    }

}
