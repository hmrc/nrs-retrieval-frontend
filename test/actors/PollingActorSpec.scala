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

import akka.pattern.ask
import org.mockito.Matchers.any
import org.mockito.Mockito._
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PollingActorSpec() extends ActorSpec {
  private val statusMessage = StatusMessage(testVaultId, testArchiveId)

  "A polling actor in poll mode" must {
    "send a PollingMessage in response to a StatusMessage" in {
      pollingActor ! RestartMessage
      Await.result(ask(pollingActor, statusMessage).mapTo[ActorMessage], defaultTimeout) should be(PollingMessage)
    }

    "change to complete mode in response to a CompleteMessage" in {
      pollingActor ! RestartMessage
      pollingActor ! CompleteMessage
      Await.result(ask(pollingActor, statusMessage).mapTo[ActorMessage], defaultTimeout) should be(CompleteMessage)
    }

    "change to failed mode in response to a FailedMessage" in {
      pollingActor ! RestartMessage
      pollingActor ! FailedMessage
      Await.result(ask(pollingActor, statusMessage).mapTo[ActorMessage], defaultTimeout) should be(FailedMessage)
    }

    "send a PollingMessage in response to a RestartMessage" in {
      pollingActor ! RestartMessage
      Await.result(ask(pollingActor, RestartMessage).mapTo[ActorMessage], defaultTimeout) should be(PollingMessage)
    }
  }

  "A polling actor in complete mode" must {
    "send a CompleteMessage in response to a StatusMessage" in {
      pollingActor ! RestartMessage
      pollingActor ! CompleteMessage
      Await.result(ask(pollingActor, statusMessage).mapTo[ActorMessage], defaultTimeout) should be(CompleteMessage)
    }

    "change to poll mode in response to a RestartMessage" in {
      pollingActor ! RestartMessage
      pollingActor ! CompleteMessage
      pollingActor ! RestartMessage

      val mockHttpResponse = mock[HttpResponse]
      when(mockHttpResponse.status).thenReturn(OK)

      when(mockNrsRetrievalConnector.statusSubmissionBundle(any(), any())(any())).thenReturn(Future(mockHttpResponse))

      Await.result(ask(pollingActor, statusMessage).mapTo[ActorMessage], 30 seconds) should be(PollingMessage)
    }
  }

  "A polling actor in failed mode" must {
    "send a FailedMessage in response to a StatusMessage" in {
      pollingActor ! RestartMessage
      pollingActor ! FailedMessage
      Await.result(ask(pollingActor, statusMessage).mapTo[ActorMessage], defaultTimeout) should be(FailedMessage)
    }

    "change to poll mode in response to a RestartMessage" in {
      pollingActor ! RestartMessage
      pollingActor ! FailedMessage
      pollingActor ! RestartMessage
      Await.result(ask(pollingActor, statusMessage).mapTo[ActorMessage], defaultTimeout) should be(PollingMessage)
    }
  }
}