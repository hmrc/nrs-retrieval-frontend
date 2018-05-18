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

package support.fixtures

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import config.AppConfig
import connectors.NrsRetrievalConnector
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.{DefaultLangs, DefaultMessagesApi}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration._

trait Infrastructure extends AnyRef with MockitoSugar {

  // concrete instances
  val environment: Environment = Environment.simple()

  val configuration: Configuration = Configuration.load(environment)

  val messageApi: DefaultMessagesApi = new DefaultMessagesApi(environment, configuration, new DefaultLangs(configuration))

  val appConfig: AppConfig = new AppConfig(configuration, environment)

  // mocks
  val mockActorRef: ActorRef = mock[ActorRef]

  val mockNrsRetrievalConnector: NrsRetrievalConnector = mock[NrsRetrievalConnector]

  val mockActorSystem:ActorSystem = mock[ActorSystem]

  val mockMaterializer: Materializer = mock[Materializer]

  val mockAppConfig: AppConfig = mock[AppConfig]
  when(mockAppConfig.futureTimeoutSeconds).thenReturn(1)
  val duration: FiniteDuration = 1000.millis
  when(mockAppConfig.interval).thenReturn(duration)
  when(mockAppConfig.runTimeMillis).thenReturn(3000)

  // implicits
  implicit val hc: HeaderCarrier = HeaderCarrier()

}
