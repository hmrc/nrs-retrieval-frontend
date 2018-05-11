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

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

// todo : use unit spec in line with our other tests
class PollingActorSpec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with MockitoSugar with Infrastructure {

  implicit val timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, TimeUnit.SECONDS))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A polling actor in poll mode" must {
    "send a PollingMessage in response to a StatusMessage for the correct vault and archive" in {
      pollingActor ! RestartMessage
      Await.result(
        ask(pollingActor, StatusMessage(testVaultId, testArchiveId)).mapTo[ActorMessage]
        , 5 seconds) should be(PollingMessage)
    }
    "send a PollingMessage in response a StatusMessage for the incorrect vault and archive" in {
      pollingActor ! RestartMessage
      Await.result(
        ask(pollingActor, StatusMessage("2", "3")).mapTo[ActorMessage]
        , 5 seconds) should be(UnknownMessage)
    }
    "change to complete mode in response to a CompleteMessage" in {
      pollingActor ! RestartMessage
      pollingActor ! CompleteMessage

      Await.result(
        ask(pollingActor, StatusMessage(testVaultId, testArchiveId)).mapTo[ActorMessage]
        , 5 seconds) should be(CompleteMessage)
    }
    "change to failed mode in response to a FailedMessage" in {
      pollingActor ! RestartMessage
      pollingActor ! FailedMessage("broken")

      Await.result(
        ask(pollingActor, StatusMessage(testVaultId, testArchiveId)).mapTo[ActorMessage]
        , 5 seconds) should be(FailedMessage("broken"))
    }
    "send a PollingMessage in response to a RestartMessage" in {
      pollingActor ! RestartMessage
      Await.result(
        ask(pollingActor, RestartMessage).mapTo[ActorMessage]
        , 5 seconds) should be(PollingMessage)
    }
  }

  "A polling actor in complete mode" must {
    "send a UnknownMessage response in response to an UnknownMessage" in {
      pollingActor ! RestartMessage
      pollingActor ! CompleteMessage
      Await.result(
        ask(pollingActor, UnknownMessage).mapTo[ActorMessage]
        , 5 seconds) should be(UnknownMessage)
    }
    "send a CompleteMessage in response to a StatusMessage for the correct vault and archive" in {
      pollingActor ! RestartMessage
      pollingActor ! CompleteMessage
      Await.result(
        ask(pollingActor, StatusMessage(testVaultId, testArchiveId)).mapTo[ActorMessage]
        , 5 seconds) should be(CompleteMessage)
    }
    "send a UnknownMessage in response a StatusMessage for the incorrect vault and archive" in {
      pollingActor ! RestartMessage
      pollingActor ! CompleteMessage
      Await.result(
        ask(pollingActor, StatusMessage("2", "3")).mapTo[ActorMessage]
        , 5 seconds) should be(UnknownMessage)
    }
    "change to poll mode in response to a RestartMessage" in {
      pollingActor ! RestartMessage
      pollingActor ! CompleteMessage
      pollingActor ! RestartMessage
      Await.result(
        ask(pollingActor, StatusMessage(testVaultId, testArchiveId)).mapTo[ActorMessage]
        , 5 seconds) should be(PollingMessage)
    }
  }

  "A polling actor in failed mode" must {
    "send a UnknownMessage response in response to an UnknownMessage" in {
      pollingActor ! RestartMessage
      pollingActor ! FailedMessage("broken")
      Await.result(
        ask(pollingActor, UnknownMessage).mapTo[ActorMessage]
        , 5 seconds) should be(UnknownMessage)
    }
    "send a FailedMessage in response to a StatusMessage for the correct vault and archive" in {
      pollingActor ! RestartMessage
      pollingActor ! FailedMessage("broken")
      Await.result(
        ask(pollingActor, StatusMessage(testVaultId, testArchiveId)).mapTo[ActorMessage]
        , 5 seconds) should be(FailedMessage("broken"))
    }
    "send a UnknownMessage in response a StatusMessage for the incorrect vault and archive" in {
      pollingActor ! RestartMessage
      pollingActor ! FailedMessage("broken")
      Await.result(
        ask(pollingActor, StatusMessage("2", "3")).mapTo[ActorMessage]
        , 5 seconds) should be(UnknownMessage)
    }
    "change to poll mode in response to a RestartMessage" in {
      pollingActor ! RestartMessage
      pollingActor ! FailedMessage("broken")
      pollingActor ! RestartMessage
      Await.result(
        ask(pollingActor, StatusMessage(testVaultId, testArchiveId)).mapTo[ActorMessage]
        , 5 seconds) should be(PollingMessage)
    }
  }

  val testVaultId: String = "1"
  val testArchiveId: String = "1"
  val pollingActor: ActorRef = system.actorOf(Props(new PollingActor(testVaultId, testArchiveId, mockAppConfig)(mockNrsRetrievalConnector)), s"pollingActor_${testArchiveId}_$testArchiveId")

}