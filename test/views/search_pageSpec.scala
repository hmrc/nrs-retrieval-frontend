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
import controllers.FormMappings
import models.SearchResult
import org.scalatest.matchers.must.Matchers._
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import support.GuiceAppSpec
import support.fixtures.{SearchFixture, ViewFixture}

class search_pageSpec extends GuiceAppSpec with SearchFixture{

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  private val searchPage = fakeApplication.injector.instanceOf[views.html.search_page]
  private val jsonBody = Json.parse("""{"searchKeyName_0": "vrn", "searchKeyValue_0": "someValue", "notableEventType": "vat-return"}""")
  private val searchForm = FormMappings.searchForm.bind(jsonBody, Int.MaxValue)

  "search page with a valid form only" should {
    "have a matching page title and header" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = searchPage(searchForm, None, None)
      doc.title mustBe doc.getElementById("pageHeader").text()
    }

    "have the correct page header" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = searchPage(searchForm, None, Some(Seq.empty[SearchResult]))
      doc.getElementById("pageHeader").text() mustBe Messages(s"search.page.vat-return.header.lbl")
    }
  }

  "search page with a valid form and no results" should {
    "not display the not found panel" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = searchPage(searchForm, None, None)
      Option(doc.getElementById("notFound")).isDefined mustBe false
    }

    "not display the results panel" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = searchPage(searchForm, None, None)
      Option(doc.getElementById("resultsFound")).isDefined mustBe false
    }
  }

  "search page with a valid form and an empty results" should {
    "display the not found panel" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = searchPage(searchForm, None, Some(Seq.empty[SearchResult]))
      Option(doc.getElementById("notFound")).isDefined mustBe true
    }

    "not display the results panel" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = searchPage(searchForm, None, Some(Seq.empty[SearchResult]))
      Option(doc.getElementById("resultsFound")).isDefined mustBe false
    }
  }

  "search page with a valid form and results" should {
    "not display the not found panel" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = searchPage(searchForm, None, Some(Seq(vatSearchResult)))
      Option(doc.getElementById("notFound")).isDefined mustBe false
    }

    "display the results panel" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = searchPage(searchForm, None, Some(Seq(vatSearchResult)))
      Option(doc.getElementById("resultsFound")).isDefined mustBe true
    }
  }

  trait SearchPageViewFixture extends ViewFixture {
    implicit val requestWithToken: FakeRequest[AnyContentAsEmpty.type] = addToken(request)
  }

}