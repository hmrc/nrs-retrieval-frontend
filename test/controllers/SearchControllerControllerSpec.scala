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

package controllers

import akka.actor.ActorRef
import play.api.libs.json.Json
import play.api.libs.json.Json.parse
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import support.fixtures.{NrsSearchFixture, SearchFixture}

class SearchControllerControllerSpec extends ControllerSpec with SearchFixture with NrsSearchFixture {
  private val getRequestWithJsonBody: FakeRequest[AnyContentAsJson] =
    getRequest.withJsonBody(
      parse("""{"searchKeyName_0": "someValue", "searchKeyValue_0": "someValue", "notableEventType": "vat-return"}"""))

  private lazy val controller = new SearchController(
    mock[ActorRef],
    mockAuthConnector,
    nrsRetrievalConnector,
    searchResultUtils,
    stubMessagesControllerComponents(),
    injector.instanceOf[views.html.search_page],
    error_template)

  "showSearchPage" should {
    "return 200" when {
      "a vat-return is specified" in {
        status(controller.showSearchPage(notableEventType = "vat-return")(getRequestWithJsonBody)) shouldBe OK
      }

      "a interest-restriction-return is specified" in {
        status(controller.showSearchPage(notableEventType = "interest-restriction-return")(getRequestWithJsonBody)) shouldBe OK
      }
    }

    "a for PPT return is specified"  in {
      status(controller.showSearchPage(notableEventType = "ppt-subscription")(getRequestWithJsonBody)) shouldBe OK
    }
  }

  "searchForm for IRR" should {
    "return no errors for IRR valid data" in {
      val postData = Json.obj("searchText" -> "someSearchText",
        "notableEventType" -> "interest-restriction-return")
      val validatedForm = FormMappings.searchForm.bind(postData)
      validatedForm.errors shouldBe empty
    }
  }

  "searchForm for PPT" should {
    "return no errors for IRR valid data" in {
      val postData = Json.obj("searchText" -> "someSearchText",
        "notableEventType" -> "ppt-subscription")
      val validatedForm = FormMappings.searchForm.bind(postData)
      validatedForm.errors shouldBe empty
    }
  }

  "searchForm" should {
    "return no errors for valid data" in {
      val postData = Json.obj("searchText" -> "someSearchText",
      "notableEventType" -> "vat-return")
      val validatedForm = FormMappings.searchForm.bind(postData)
      validatedForm.errors shouldBe empty
    }

    "create a header carrier with X-API-Key when one exists in config" in {
      controller.hc(getRequestWithJsonBody).headers should contain ("X-API-Key" -> appConfig.xApiKey)
    }
  }
}


