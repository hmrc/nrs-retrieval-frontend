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

import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{JsValue, Json}
import play.twirl.api.HtmlFormat
import config.AppConfig
import controllers.FormMappings
import models.{Search, SearchResult, SearchResults}
import support.GuiceAppSpec
import support.fixtures.{SearchFixture, ViewFixture}
import views.html._

class search_pageSpec extends GuiceAppSpec with SearchFixture{


  "search page with a valid form only" should {
    val jsonBody: JsValue = Json.parse("""{"query": {"searchQuery": "someValue"},"results":{"results": [],"resultCount": 0}}""")
    val searchForm: Form[Search] = FormMappings.searchForm.bind(jsonBody)
    "have a matching page title and header" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = search_page(searchForm, notableEventType = "vat-return")
      doc.title mustBe doc.getElementById("pageHeader").text()
    }

    "have the correct page header" in new SearchPageViewFixture {
      val notableEventType: String = "vat-return"
      val view: HtmlFormat.Appendable = search_page(searchForm, notableEventType = notableEventType)
      doc.getElementById("pageHeader").text() mustBe Messages(s"search.page.$notableEventType.header.lbl")
    }
  }

  "search page with a valid form and no results" should {
    val jsonBody: JsValue = Json.parse("""{"query": {"searchQuery": "someValue"}}""")
    val searchForm: Form[Search] = FormMappings.searchForm.bind(jsonBody)

    "not display the not found panel" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = search_page(searchForm, notableEventType = "vat-return")
      Option(doc.getElementById("notFound")).isDefined mustBe false
    }

    "not display the results panel" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = search_page(searchForm, notableEventType = "vat-return")
      Option(doc.getElementById("resultsFound")).isDefined mustBe false
    }
  }

  "search page with a valid form and an empty results" should {
    val jsonBody: JsValue = Json.parse("""{"query": {"searchQuery": "someValue"},"results":{"results": [],"resultCount": 0}}""")
    val searchForm: Form[Search] = FormMappings.searchForm.bind(jsonBody)
    val searchResults: Option[SearchResults] = Some(models.SearchResults(Seq.empty[SearchResult], 1))

    "display the not found panel" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = search_page(searchForm, notableEventType = "vat-return")
      Option(doc.getElementById("notFound")).isDefined mustBe true
    }

    "not display the results panel" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = search_page(searchForm, notableEventType = "vat-return")
      Option(doc.getElementById("resultsFound")).isDefined mustBe false
    }
  }

  "search page with a valid form and results" should {
    val jsonBody: JsValue = Json.parse("""{"query": {"searchQuery": "someValue"},"results":{"results": [{"notableEventDisplayName": "notableEvent", "fileDetails": "filename", "vaultId": "vaultId", "archiveId": "archiveId", "submissionDateEpochMilli": 1}],"resultCount": 0}}""")
    val searchForm: Form[Search] = FormMappings.searchForm.bind(jsonBody)

    "not display the not found panel" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = search_page(searchForm, None, notableEventType = "vat-return")
      Option(doc.getElementById("notFound")).isDefined mustBe false
    }

    "display the results panel" in new SearchPageViewFixture {
      val view: HtmlFormat.Appendable = search_page(searchForm, None, notableEventType = "vat-return")
      Option(doc.getElementById("resultsFound")).isDefined mustBe true
    }
  }

  trait SearchPageViewFixture extends ViewFixture {
    implicit val requestWithToken = addToken(request)
  }

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

}