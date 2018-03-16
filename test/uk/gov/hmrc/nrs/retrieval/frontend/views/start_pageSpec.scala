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

package uk.gov.hmrc.nrs.retrieval.frontend.views

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
import uk.gov.hmrc.nrs.retrieval.frontend.config.AppConfig
import uk.gov.hmrc.nrs.retrieval.frontend.support.GuiceAppSpec
import uk.gov.hmrc.nrs.retrieval.frontend.support.fixtures.ViewFixture
import uk.gov.hmrc.nrs.retrieval.frontend.views.html.start_page

class start_pageSpec extends GuiceAppSpec {

  "start page" should {
    "have the correct title and GA page view event" in new StartPageViewFixture {
      val view: HtmlFormat.Appendable = start_page()
      doc.title mustBe Messages("start.page.title.lbl")
    }

    "have the correct page header" in new StartPageViewFixture {
      val view: HtmlFormat.Appendable = start_page()
      doc.getElementById("pageHeader").text() mustBe Messages("start.page.header.lbl")
    }

    "include the service scope table" in new StartPageViewFixture {
      val view: HtmlFormat.Appendable = start_page()
      doc.getElementById("serviceScope").hasText mustBe true
    }

    "have the correct continue button" in new StartPageViewFixture {
      val view: HtmlFormat.Appendable = start_page()
      doc.getElementById("continueButton").text() mustBe Messages("start.button.start.lbl")
    }
  }

  trait StartPageViewFixture extends ViewFixture {
    implicit val requestWithToken = addToken(request)
  }

  implicit val appConfig = app.injector.instanceOf[AppConfig]


}