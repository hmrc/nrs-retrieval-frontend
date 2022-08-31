/*
 * Copyright 2022 HM Revenue & Customs
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

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.testkit.TestActors.echoActorProps
import models.AuthorisedUser
import org.mockito.Matchers.any
import org.mockito.Mockito._
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.{Await, Future}

class RetrievalActorSpec() extends ActorSpec {

  "A retrieval actor" must {
    val testUser = AuthorisedUser("aUser", "anAuthProviderId")
    val echo = system.actorOf(echoActorProps)
    val mockPollingActorService = mock[ActorService]

    "submit a retrieval request on receiving a SubmitRequest message" in {
      when(mockPollingActorService.pollingActorExists(any(), any())(any(), any())).thenReturn(Future(true))
      when(mockPollingActorService.pollingActor(any(), any())(any(), any())).thenReturn(Future.successful(echo))

      val mockHttpResponse = mock[HttpResponse]
      when(mockNrsRetrievalConnector.submitRetrievalRequest(any(), any(), any())(any())).thenReturn(Future(mockHttpResponse))
      when(mockHttpResponse.status).thenReturn(ACCEPTED)

      val retrievalActor: ActorRef =
        system.actorOf(Props(new RetrievalActor(mockAppConfig, mockPollingActorService)(mockNrsRetrievalConnector, executionContext)))

      Await.result(
        Await.result(
          ask(retrievalActor, SubmitMessage(testVaultId, testArchiveId, hc, testUser)).mapTo[Future[ActorMessage]]
          , defaultTimeout)
        , defaultTimeout) should be(PollingMessage)
    }

    "respond to an IsComplete message with a CompleteMessage when polling has completed" in {
      when(mockPollingActorService.eventualPollingActor(any(), any())(any(), any())).thenReturn(Future.successful(echo))

      val retrievalActor: ActorRef =
        system.actorOf(Props(new RetrievalActor(mockAppConfig, mockPollingActorService)(mockNrsRetrievalConnector, executionContext)))

      Await.result(
        Await.result(
          ask(retrievalActor, IsCompleteMessage(testVaultId, testArchiveId)).mapTo[Future[ActorMessage]]
          , defaultTimeout)
        , defaultTimeout) should be(IsCompleteMessage(testVaultId, testArchiveId))
    }
  }
}