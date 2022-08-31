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

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.TestKit.shutdownActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import config.AppConfig
import connectors.NrsRetrievalConnector
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import support.UnitSpec

import java.util.concurrent.TimeUnit.SECONDS
import scala.concurrent.duration.{FiniteDuration, _}

class ActorSpec extends TestKit(ActorSystem("MySpec")) with ImplicitSender with UnitSpec with BeforeAndAfterAll {
  implicit val timeout: Timeout = Timeout(FiniteDuration(appConfig.futureTimeoutSeconds, SECONDS))

  val testVaultId: String =  "1"
  val testArchiveId: String = "1"

  val mockAppConfig: AppConfig = mock[AppConfig]
  when(mockAppConfig.futureTimeoutSeconds).thenReturn(1)
  when(mockAppConfig.interval).thenReturn(1000.millis)
  when(mockAppConfig.runTimeMillis).thenReturn(3000)

  val mockNrsRetrievalConnector: NrsRetrievalConnector = mock[NrsRetrievalConnector]

  val pollingActor: ActorRef = system.actorOf(
    Props(new PollingActor(testVaultId, testArchiveId, mockAppConfig)(mockNrsRetrievalConnector, executionContext)),
    s"pollingActor_${testArchiveId}_$testArchiveId")

  override def afterAll: Unit = shutdownActorSystem(system)
}
