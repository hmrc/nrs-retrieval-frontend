/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.nrsretrievalfrontend.support

import org.apache.pekko.actor.ActorSystem
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

trait UnitSpec extends AnyWordSpecLike with Matchers with MockitoSugar with Status with Configs:
  given defaultTimeout: FiniteDuration = 5 seconds

  given executionContext: ExecutionContext = ExecutionContext.Implicits.global

  given actorSystem: ActorSystem = ActorSystem.create("test")

  given hc: HeaderCarrier = HeaderCarrier()

  def await[A](future: Future[A])(using timeout: Duration): A = Await.result(future, timeout)
