/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers._
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.ViewSpec.ensureCommonPageElementsAreRendered
import views.html.start_page

class start_pageSpec extends ViewSpec {
  private lazy val startPage = injector.instanceOf[start_page]
  override lazy val view: HtmlFormat.Appendable = startPage()

  "start page" should {
    "render correctly" in {
      val headerText = Messages("start.page.header.lbl")

      ensureCommonPageElementsAreRendered(doc = doc, headerText = headerText, titleText = headerText, maybeUserName = None)
      doc.getElementById("continueButton").text() mustBe Messages("start.button.start.lbl")
    }
  }
}