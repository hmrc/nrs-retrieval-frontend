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

package uk.gov.hmrc.nrsretrievalfrontend.controllers

import org.scalatest.mockito.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.data.FormError
import play.api.http.Status
import play.api.i18n.{DefaultLangs, DefaultMessagesApi}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nrsretrievalfrontend.config.AppConfig
import uk.gov.hmrc.nrsretrievalfrontend.connectors.NrsRetrievalConnector
import uk.gov.hmrc.nrsretrievalfrontend.support.fixtures.{NrsSearchFixture, SearchFixture}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class SearchControllerControllerSpec extends UnitSpec with WithFakeApplication with MockitoSugar with SearchFixture with NrsSearchFixture {

  "GET /" should {
    "return 200" in {
      val result = controller.showSearchPage(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }

  "submitSearchPage" should {
    "return 200" in {
      when(mockNRC.search(any[String])(any[HeaderCarrier])).thenReturn(Future.successful(Seq(nrsSearchResult)))
      val result = controller.submitSearchPage(fakeRequest.withJsonBody(searchFormJson))
      status(result) shouldBe Status.OK
    }

    "return 400 BadRequest" in {
      val result = controller.submitSearchPage(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }
  }

  "searchForm" should {
    "return no errors for valid data" in {
      val postData = Json.obj("searchText" -> "someSearchText")
      val validatedForm = controller.searchForm.bind(postData)
      validatedForm.errors shouldBe empty
    }
    "return errors for missing data" in {
      val postData = Json.obj()
      val validatedForm = controller.searchForm.bind(postData)
      validatedForm.errors shouldBe List(FormError("searchText",List("error.required")))
    }
  }

  private val fakeRequest = FakeRequest("GET", "/")

  private val env = Environment.simple()
  private val configuration = Configuration.load(env)

  private val messageApi = new DefaultMessagesApi(env, configuration, new DefaultLangs(configuration))
  private val appConfig = new AppConfig(configuration, env)
  private val mockNRC = mock[NrsRetrievalConnector]
  private val controller = new SearchController(messageApi, mockNRC, appConfig)

}


