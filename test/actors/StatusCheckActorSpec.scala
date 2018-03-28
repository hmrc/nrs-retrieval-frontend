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

// todo : use unit spec in line with our other tests
class StatusCheckActorSpec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with MockitoSugar with Infrastructure {

  implicit val timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A check status actor when the retrieval is complete" must {
    "send an CompleteMessage to the polling actor" in {
      val mockRetrievalRequestHttpResponse = mock[HttpResponse]
      when(mockRetrievalRequestHttpResponse.status).thenReturn(Status.OK)
      when(mockNrsRetrievalConnector.statusSubmissionBundle(any(), any())(any()))
        .thenReturn(Future.successful(mockRetrievalRequestHttpResponse))

      pollingActor ! RestartMessage

      checkStatusActor ! StatusMessage(testVaultId, testArchiveId)

      Thread.sleep(1000) // sleep to allow all oof the asynch ops to catch up

      Await.result(
        ask(pollingActor, StatusMessage(testVaultId, testArchiveId)).mapTo[ActorMessage]
        , 5 seconds) should be(CompleteMessage)
    }
  }

  "A check status actor when the retrieval is failed" must {
    "send an FailedMessage to the polling actor" in {
      val mockRetrievalRequestHttpResponse = mock[HttpResponse]
      when(mockRetrievalRequestHttpResponse.status).thenReturn(Status.BAD_REQUEST)
      when(mockNrsRetrievalConnector.statusSubmissionBundle(any(), any())(any()))
        .thenReturn(Future.successful(mockRetrievalRequestHttpResponse))

      pollingActor ! RestartMessage

      checkStatusActor ! StatusMessage(testVaultId, testArchiveId)

      Thread.sleep(1000) // sleep to allow all oof the asynch ops to catch up

      Await.result(
        ask(pollingActor, StatusMessage(testVaultId, testArchiveId)).mapTo[ActorMessage]
        , 5 seconds) should be(FailedMessage(Status.BAD_REQUEST.toString))
    }
  }

  "A check status actor when the retrieval is in progress" must {
    "send an FailedMessage to the polling actor" in {
      val mockRetrievalRequestHttpResponse = mock[HttpResponse]
      when(mockRetrievalRequestHttpResponse.status).thenReturn(Status.NOT_FOUND)
      when(mockNrsRetrievalConnector.statusSubmissionBundle(any(), any())(any()))
        .thenReturn(Future.successful(mockRetrievalRequestHttpResponse))

      pollingActor ! RestartMessage

      checkStatusActor ! StatusMessage(testVaultId, testArchiveId)

      Thread.sleep(1000) // sleep to allow all oof the asynch ops to catch up

      Await.result(
        ask(pollingActor, StatusMessage(testVaultId, testArchiveId)).mapTo[ActorMessage]
        , 5 seconds) should be(PollingMessage)
    }
  }

  val testVaultId: Long = 1
  val testArchiveId: Long = 1

  val pollingActor: ActorRef = system.actorOf(Props(new PollingActor(testVaultId, testArchiveId)(mockNrsRetrievalConnector)), s"pollingActor_${testArchiveId}_$testArchiveId")

  val checkStatusActor: ActorRef = system.actorOf(Props(new CheckStatusActor(pollingActor.path)(mockNrsRetrievalConnector)))

}