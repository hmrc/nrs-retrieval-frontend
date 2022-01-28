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

import akka.actor.Props
import akka.pattern.ask
import akka.testkit.TestActorRef
import org.mockito.Matchers.any
import org.mockito.Mockito._
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.{Await, Future}

class StatusCheckActorSpec() extends ActorSpec {
  private val checkStatusActor =
    TestActorRef[CheckStatusActor](Props(new CheckStatusActor(mockAppConfig)(mockNrsRetrievalConnector)), pollingActor.actorRef)
  private val mockRetrievalRequestHttpResponse = mock[HttpResponse]
  private val statusMessage = StatusMessage(testVaultId, testArchiveId)
  private val ONE_SECOND = 1000

  // sleep to allow all oof the async ops to catch up
  private def sleep(): Unit = Thread.sleep(ONE_SECOND)

  "A check status actor when the retrieval is complete" must {
    "send an CompleteMessage to the polling actor" in {
      when(mockRetrievalRequestHttpResponse.status).thenReturn(OK)
      when(mockNrsRetrievalConnector.statusSubmissionBundle(any(), any())(any()))
        .thenReturn(Future.successful(mockRetrievalRequestHttpResponse))

      pollingActor ! RestartMessage
      checkStatusActor ! statusMessage

      sleep()

      Await.result(ask(pollingActor, statusMessage).mapTo[ActorMessage], defaultTimeout) should be(CompleteMessage)
    }
  }

  "A check status actor when the retrieval is failed" must {
    "send an FailedMessage to the polling actor" in {
      when(mockRetrievalRequestHttpResponse.status).thenReturn(BAD_REQUEST)
      when(mockNrsRetrievalConnector.statusSubmissionBundle(any(), any())(any()))
        .thenReturn(Future.successful(mockRetrievalRequestHttpResponse))

      pollingActor ! RestartMessage
      checkStatusActor ! statusMessage

      sleep()

      Await.result(ask(pollingActor, statusMessage).mapTo[ActorMessage], defaultTimeout) should be(FailedMessage)
    }
  }

  "A check status actor when the retrieval is in progress" must {
    "send an FailedMessage to the polling actor" in {
      val mockRetrievalRequestHttpResponse = mock[HttpResponse]
      when(mockRetrievalRequestHttpResponse.status).thenReturn(NOT_FOUND)
      when(mockNrsRetrievalConnector.statusSubmissionBundle(any(), any())(any()))
        .thenReturn(Future.successful(mockRetrievalRequestHttpResponse))

      pollingActor ! RestartMessage
      checkStatusActor ! statusMessage

      sleep()

      Await.result(ask(pollingActor, statusMessage).mapTo[ActorMessage], defaultTimeout) should be(PollingMessage)
    }
  }
}