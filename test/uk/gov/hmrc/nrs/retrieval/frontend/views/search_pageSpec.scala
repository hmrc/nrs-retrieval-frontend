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

import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.nrs.retrieval.frontend.config.AppConfig
import uk.gov.hmrc.nrs.retrieval.frontend.controllers.SearchController
import uk.gov.hmrc.nrs.retrieval.frontend.model.SearchQuery
import uk.gov.hmrc.nrs.retrieval.frontend.support.GuiceAppSpec
import uk.gov.hmrc.nrs.retrieval.frontend.support.fixtures.ViewFixture
import uk.gov.hmrc.nrs.retrieval.frontend.views.html._

//class start_pageSpec extends GuiceAppSpec with TestUtil with TestAppConfig {
class search_pageSpec extends GuiceAppSpec {

  trait SearchPageViewFixture extends ViewFixture {
    implicit val requestWithToken = addToken(request)
  }

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  private val postData = Json.obj("searchText" -> "someSearchText")
  val searchQueryForm: Form[SearchQuery] = SearchController.searchForm.bind(postData)

  "select_client view" should {

    "have the correct title and GA page view event" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = search_page(searchQueryForm)
      doc.title mustBe Messages("search.page.title.lbl")
    }
 }

}