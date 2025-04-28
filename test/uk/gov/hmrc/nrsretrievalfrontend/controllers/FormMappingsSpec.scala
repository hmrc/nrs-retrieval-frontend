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

package uk.gov.hmrc.nrsretrievalfrontend.controllers

import uk.gov.hmrc.nrsretrievalfrontend.controllers.FormMappings.form
import play.api.libs.json.Json
import uk.gov.hmrc.nrsretrievalfrontend.support.UnitSpec

class FormMappingsSpec extends UnitSpec:
  appConfig.notableEvents.foreach { notableEvent =>
    "searchForm" should {
      s"bind for for ${notableEvent._1}" in {
        val postData      = Json.obj("searchText" -> "someSearchText", "notableEventType" -> notableEvent._2.name)
        val validatedForm = form.bind(postData, Integer.MAX_VALUE)
        validatedForm.errors shouldBe empty
      }
    }
  }
