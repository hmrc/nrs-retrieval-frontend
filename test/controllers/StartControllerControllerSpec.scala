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

package controllers

import akka.actor.ActorSystem
import config.AppConfig
import play.api.http.Status
import play.api.i18n.{DefaultLangs, DefaultMessagesApi}
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import support.fixtures.StrideFixture
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class StartControllerControllerSpec extends UnitSpec with WithFakeApplication with StrideFixture {

  private val fakeRequest = FakeRequest("GET", "/")

  private val env = Environment.simple()
  private val configuration = Configuration.load(env)

  private val messageApi = new DefaultMessagesApi(env, configuration, new DefaultLangs(configuration))
  private val appConfig = new AppConfig(configuration, env)
  private val mockActorSystem = mock[ActorSystem]

  private class StartControllerWithAuth(stubbedRetrievalResult: Future[_])
    extends StartController(messageApi, mockActorSystem, appConfig, mockAuthConn) {

    override val authConnector = authConnOk(stubbedRetrievalResult)

  }

  private val controller = new StartControllerWithAuth(authResultOk)

  "GET /" should {
    "return 200" in {
      val result = controller.showStartPage(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }


}
