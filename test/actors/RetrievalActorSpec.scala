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
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import play.api.http.Status
import support.fixtures.Infrastructure
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

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

  "A retrieval actor in response to a SubmitMessage" must {
    "send a FailedToStartMessage when no polling actor exists and gets BAD_REQUEST from the connector" in {
      val mockPollingActor = mock[ActorRef]

      val mockPollingActorService = mock[ActorService]
      when(mockPollingActorService.maybePollingActor(any(), any())(any(), any())).thenReturn(None)
      when(mockPollingActorService.startPollingActor(any(), any())(any(), any())).thenReturn(mockPollingActor)

      val mockRetrievalRequestHttpResponse = mock[HttpResponse]
      when(mockRetrievalRequestHttpResponse.status).thenReturn(Status.BAD_REQUEST)

      when(mockNrsRetrievalConnector.submitRetrievalRequest(any(), any())(any()))
        .thenReturn(Future.successful(mockRetrievalRequestHttpResponse))

      val retrievalActor: ActorRef = system.actorOf(Props(new RetrievalActor(mockAppConfig, mockPollingActorService)(mockNrsRetrievalConnector)))

      Await.result(
        Await.result(
          ask(retrievalActor, SubmitMessage(testVaultId, testArchiveId)).mapTo[Future[ActorMessage]]
          , 5 seconds)
        , 5 seconds) should be(FailedToStartMessage)
    }

    "send a FailedToStartMessage when no polling actor exists and gets OK from the connector" in {
      val mockPollingActor = mock[ActorRef]

      val mockPollingActorService = mock[ActorService]
      when(mockPollingActorService.maybePollingActor(any(), any())(any(), any())).thenReturn(None)
      when(mockPollingActorService.startPollingActor(any(), any())(any(), any())).thenReturn(mockPollingActor)

      val mockRetrievalRequestHttpResponse = mock[HttpResponse]
      when(mockRetrievalRequestHttpResponse.status).thenReturn(Status.OK)

      when(mockNrsRetrievalConnector.submitRetrievalRequest(any(), any())(any()))
        .thenReturn(Future.successful(mockRetrievalRequestHttpResponse))

      val retrievalActor: ActorRef = system.actorOf(Props(new RetrievalActor(mockAppConfig, mockPollingActorService)(mockNrsRetrievalConnector)))

      Await.result(
        Await.result(
          ask(retrievalActor, SubmitMessage(testVaultId, testArchiveId)).mapTo[Future[ActorMessage]]
          , 5 seconds)
        , 5 seconds) should be(StartedMessage)
    }
  }

  "A retrieval actor in response to a StatusMessage" must {
    "send an UnknownMessage response when no polling actor exists" in {
      val mockPollingActorService = mock[ActorService]
      when(mockPollingActorService.maybePollingActor(any(), any())(any(), any())).thenReturn(None)

      val retrievalActor: ActorRef = system.actorOf(Props(new RetrievalActor(mockAppConfig, mockPollingActorService)(mockNrsRetrievalConnector)))

      Await.result(
        Await.result(
          ask(retrievalActor, UnknownMessage).mapTo[Future[ActorMessage]]
          , 5 seconds)
        , 5 seconds) should be(UnknownMessage)

    }
  }

  val testVaultId: String = "1"
  val testArchiveId: String = "1"

}