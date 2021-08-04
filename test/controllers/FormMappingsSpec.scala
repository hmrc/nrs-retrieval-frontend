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

import controllers.FormMappings.searchForm
import play.api.libs.json.Json
import support.UnitSpec

import java.lang.Integer.MAX_VALUE

class FormMappingsSpec extends UnitSpec {
  "searchForm for IRR" should {
    "return no errors for IRR valid data" in {
      val postData = Json.obj("searchText" -> "someSearchText", "notableEventType" -> "interest-restriction-return")
      val validatedForm = searchForm.bind(postData, MAX_VALUE)
      validatedForm.errors shouldBe empty
    }
  }

  "searchForm for PPT" should {
    "return no errors for IRR valid data" in {
      val postData = Json.obj("searchText" -> "someSearchText", "notableEventType" -> "ppt-subscription")
      val validatedForm = searchForm.bind(postData, MAX_VALUE)
      validatedForm.errors shouldBe empty
    }
  }

  "searchForm" should {
    "return no errors for valid data" in {
      val postData = Json.obj("searchText" -> "someSearchText", "notableEventType" -> "vat-return")
      val validatedForm = searchForm.bind(postData, MAX_VALUE)
      validatedForm.errors shouldBe empty
    }
  }
}
