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
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import akka.util.Timeout
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import play.api.http.Status._
import support.fixtures.Infrastructure
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class RetrievalActorSpec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with MockitoSugar with Infrastructure {

  implicit val timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A retrieval actor" must {

    "submit a retrieval request on receiving a SubmitRequest message" in {
      val echo = system.actorOf(TestActors.echoActorProps)

      val mockPollingActorService = mock[ActorService]
      when(mockPollingActorService.pollingActorExists(any(), any())(any(), any())).thenReturn(Future(true))
      when(mockPollingActorService.pollingActor(any(), any())(any(), any())).thenReturn(Future.successful(echo))

      val mockHttpResponse = mock[HttpResponse]
      when(mockNrsRetrievalConnector.submitRetrievalRequest(any(), any())(any())).thenReturn(Future(mockHttpResponse))
      when(mockHttpResponse.status).thenReturn(ACCEPTED)

      val retrievalActor: ActorRef = system.actorOf(Props(new RetrievalActor(mockAppConfig, mockPollingActorService)(mockNrsRetrievalConnector)))

      Await.result(
        Await.result(
          ask(retrievalActor, SubmitMessage(testVaultId, testArchiveId, hc)).mapTo[Future[ActorMessage]]
          , 5 seconds)
        , 5 seconds) should be(PollingMessage)
    }

    "respond to an IsComplete message with a CompleteMessage when polling has completed" in {
      val echo = system.actorOf(TestActors.echoActorProps)

      val mockPollingActorService = mock[ActorService]
      when(mockPollingActorService.eventualPollingActor(any(), any())(any(), any())).thenReturn(Future.successful(echo))

      val retrievalActor: ActorRef = system.actorOf(Props(new RetrievalActor(mockAppConfig, mockPollingActorService)(mockNrsRetrievalConnector)))

      Await.result(
        Await.result(
          ask(retrievalActor, IsCompleteMessage(testVaultId, testArchiveId)).mapTo[Future[ActorMessage]]
          , 5 seconds)
        , 5 seconds) should be(IsCompleteMessage(testVaultId, testArchiveId))
    }
  }

  val testVaultId: String = "1"
  val testArchiveId: String = "1"

}