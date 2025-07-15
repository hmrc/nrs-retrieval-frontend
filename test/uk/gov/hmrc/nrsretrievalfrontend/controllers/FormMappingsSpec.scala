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

import play.api.libs.json.Json
import uk.gov.hmrc.nrsretrievalfrontend.controllers.FormMappings.form
import uk.gov.hmrc.nrsretrievalfrontend.support.UnitSpec

class FormMappingsSpec extends UnitSpec:
  appConfig.notableEvents.foreach { (notableEventName, notableEvent) =>
    s"searchForm - $notableEventName" should {
      s"bind with data" in {
        val fields        = notableEvent.searchKeys.zipWithIndex.map((s, idx) => s"queries[$idx].name" -> Json.toJsFieldJsValueWrapper(s.label)) ++
          notableEvent.searchKeys.zipWithIndex.map((s, idx) => s"queries[$idx].value" -> Json.toJsFieldJsValueWrapper(s.name)) :+
          ("notableEventType" -> Json.toJsFieldJsValueWrapper(notableEvent.name))
        val postData      = Json.obj(fields*)
        val validatedForm = form.bind(postData, Integer.MAX_VALUE)
        validatedForm.errors shouldBe empty
      }

      s"bind with no data" in {
        val fields        = notableEvent.searchKeys.zipWithIndex.map((s, idx) => s"queries[$idx].name" -> Json.toJsFieldJsValueWrapper(s.label)) ++
          notableEvent.searchKeys.zipWithIndex.map((_, idx) => s"queries[$idx].value" -> Json.toJsFieldJsValueWrapper("")) :+
          ("notableEventType" -> Json.toJsFieldJsValueWrapper(notableEvent.name))
        val postData      = Json.obj(fields*)
        val validatedForm = form.bind(postData, Integer.MAX_VALUE)
        validatedForm.errors.flatMap(_.messages) should not be empty
      }
    }
  }
