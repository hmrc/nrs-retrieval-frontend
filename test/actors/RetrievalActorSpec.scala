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

import akka.actor.{ActorNotFound, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import support.fixtures.Infrastructure

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

// todo : use unit spec in line with our other tests
class RetrievalActorSpec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with MockitoSugar with Infrastructure {

  implicit val timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A retrieval actor in response to an UnknownMessage" must {
    "send an UnknownMessage response" in {
      val mockPollingActorService = mock[ActorService]
      val retrievalActor: ActorRef = system.actorOf(Props(new RetrievalActor(mockAppConfig, mockPollingActorService)(mockNrsRetrievalConnector)))

      Await.result(
        Await.result(
          ask(retrievalActor, UnknownMessage).mapTo[Future[ActorMessage]]
          , 5 seconds)
        , 5 seconds) should be(UnknownMessage)

    }
  }

  "A retrieval actor" must {
    "send an UnknownMessage response when no polling actor exists and an UnknownMessage is received" in {
      val mockPollingActorService = mock[ActorService]
      when(mockPollingActorService.pollingActor(any(), any())(any(), any())).thenReturn(Future.failed(new Throwable))

      val retrievalActor: ActorRef = system.actorOf(Props(new RetrievalActor(mockAppConfig, mockPollingActorService)(mockNrsRetrievalConnector)))

      Await.result(
        Await.result(
          ask(retrievalActor, UnknownMessage).mapTo[Future[ActorMessage]]
          , 5 seconds)
        , 5 seconds) should be(UnknownMessage)

    }

    "send an UnknownMessage response when no polling actor exists and a StatusMessage is received" in {
      val mockPollingActorService = mock[ActorService]
      when(mockPollingActorService.eventualPollingActor(any(), any())(any(), any())).thenReturn(Future.failed(new Throwable))

      val retrievalActor: ActorRef = system.actorOf(Props(new RetrievalActor(mockAppConfig, mockPollingActorService)(mockNrsRetrievalConnector)))

      Await.result(
        Await.result(
          ask(retrievalActor, StatusMessage(testVaultId, testArchiveId)).mapTo[Future[ActorMessage]]
          , 5 seconds)
        , 5 seconds) should be(UnknownMessage)

    }
  }

  val testVaultId: String = "1"
  val testArchiveId: String = "1"

}