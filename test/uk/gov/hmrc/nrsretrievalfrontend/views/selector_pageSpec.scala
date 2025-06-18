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

package uk.gov.hmrc.nrsretrievalfrontend.views

import org.scalatest.matchers.must.Matchers.*
import play.api.i18n.Messages
import play.api.libs.json.Json.parse
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.nrsretrievalfrontend.controllers.FormMappings.selectorForm
import uk.gov.hmrc.nrsretrievalfrontend.controllers.routes
import uk.gov.hmrc.nrsretrievalfrontend.models.NotableEvent
import uk.gov.hmrc.nrsretrievalfrontend.support.ViewSpec
import uk.gov.hmrc.nrsretrievalfrontend.support.ViewSpec.{elementByName, ensureCommonPageElementsAreRendered}

class selector_pageSpec extends ViewSpec:

  override lazy val view: HtmlFormat.Appendable =
    selectorPage(selectorForm.bind(parse("""{"notableEventType": "vat-return"}"""), Int.MaxValue))

  "selector page" should {
    "render correctly" in {
      val headerText = Messages("selector.page.header.lbl")

      ensureCommonPageElementsAreRendered(
        doc = doc,
        headerText = headerText,
        titleText = headerText,
        maybeBackLinkCall = Some(routes.StartController.showStartPage)
      )

      elementByName(doc, "continueButton").text() mustBe Messages("button.continue.lbl")

      indexedNotableEvents.map { case (notableEvent: NotableEvent, index: Int) =>
        val notableEventTypeInputIdSuffix = if index > 0 then s"-${index + 1}" else ""
        val notableEventTypeInput         = doc.getElementById(s"notableEventType$notableEventTypeInputIdSuffix")

        notableEventTypeInput.attr("type") mustBe "radio"
        notableEventTypeInput.attr("value") mustBe notableEvent.name
      }
    }
  }
