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

import config.AppConfig
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import support.GuiceAppSpec
import support.fixtures.ViewFixture
import views.html.error_template

class error_pageSpec extends GuiceAppSpec {

  "error page" should {
    "have the correct title" in new StartPageViewFixture {
      val view: HtmlFormat.Appendable = error_template(
        Messages("global.error.InternalServerError500.title"),
        Messages("global.error.InternalServerError500.heading"),
        Messages("global.error.InternalServerError500.message")
      )
      doc.title mustBe Messages("global.error.InternalServerError500.title")
    }
  }

  trait StartPageViewFixture extends ViewFixture {
    implicit val requestWithToken = addToken(request)
  }

  implicit val appConfig = app.injector.instanceOf[AppConfig]


}