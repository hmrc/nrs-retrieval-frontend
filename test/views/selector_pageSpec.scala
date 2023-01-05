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

package views

import controllers.FormMappings.selectorForm
import models.NotableEvent
import org.scalatest.matchers.must.Matchers._
import play.api.i18n.Messages
import play.api.libs.json.Json.parse
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.html.components.{FormWithCSRF, GovukButton, GovukErrorMessage, GovukFieldset, GovukHint, GovukLabel, GovukRadios}
import views.ViewSpec.{elementByName, ensureCommonPageElementsAreRendered, someUser}
import views.html.components.Button
import views.html.selector_page

class selector_pageSpec extends ViewSpec {
  private lazy val page = new selector_page(
    layout,
    new FormWithCSRF,
    new GovukRadios(new GovukErrorMessage, new GovukFieldset, new GovukHint, new GovukLabel),
    new Button(new GovukButton)
  )

  override lazy val view: HtmlFormat.Appendable =
    page(selectorForm.bind(parse("""{"notableEventType": "vat-return"}"""), Int.MaxValue), someUser)

  "selector page" should {
    "render correctly" in {
      val headerText = Messages("selector.page.header.lbl")

      ensureCommonPageElementsAreRendered(
        doc = doc,
        headerText = headerText,
        titleText = headerText,
        maybeBackLinkCall = Some(controllers.routes.StartController.showStartPage))

      elementByName(doc, "continueButton").text() mustBe Messages("button.continue.lbl")

      indexedNotableEvents.map{ case (notableEvent: NotableEvent, index: Int ) =>
        val notableEventTypeInputIdSuffix = if (index > 0) s"-${index + 1}" else ""
        val notableEventTypeInput = doc.getElementById(s"notableEventType$notableEventTypeInputIdSuffix")

        notableEventTypeInput.attr("type") mustBe "radio"
        notableEventTypeInput.attr("value") mustBe notableEvent.name
      }
    }
  }
}