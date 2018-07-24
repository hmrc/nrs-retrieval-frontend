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

package views

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

import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import config.AppConfig
import support.GuiceAppSpec
import support.fixtures.ViewFixture
import views.html.start_page

class start_pageSpec extends GuiceAppSpec {

  "start page" should {
    "have a matching page title and h1 header" in new StartPageViewFixture {
      val view: HtmlFormat.Appendable = start_page()
      doc.title mustBe doc.getElementById("pageHeader").text()
    }

    "have the correct page header" in new StartPageViewFixture {
      val view: HtmlFormat.Appendable = start_page()
      doc.getElementById("pageHeader").text() mustBe Messages("start.page.header.lbl")
    }

    "have the correct start button" in new StartPageViewFixture {
      val view: HtmlFormat.Appendable = start_page()
      doc.getElementsByClass("button--get-started").text() mustBe Messages("start.button.start.lbl")
    }
  }

  trait StartPageViewFixture extends ViewFixture {
    implicit val requestWithToken = addToken(request)
  }

  implicit val appConfig = app.injector.instanceOf[AppConfig]


}