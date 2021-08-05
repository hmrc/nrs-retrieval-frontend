/*
 * Copyright 2021 HM Revenue & Customs
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

package support

import config.AppConfig
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Await, Future}

trait UnitSpec extends AnyWordSpecLike with Matchers with MockitoSugar with Status {
  implicit val defaultTimeout: FiniteDuration = 5 seconds

  val environment: Environment = Environment.simple()
  val configuration: Configuration = Configuration.load(environment)
  val servicesConfig = new ServicesConfig(configuration)

  implicit val appConfig: AppConfig = new AppConfig(configuration, environment, servicesConfig)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)
}
