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

import controllers.FormMappings.searchForm
import models.{NotableEvent, SearchResult}
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.Json.parse
import play.twirl.api.HtmlFormat
import support.BaseUnitSpec
import support.fixtures.SearchFixture
import uk.gov.hmrc.govukfrontend.views.html.components.{FormWithCSRF, GovukButton, GovukErrorMessage, GovukHint, GovukInput, GovukLabel}
import views.ViewSpec.{elementByName, ensureCommonPageElementsAreRendered, someUser}
import views.html.components.{Button, Paragraph, SearchResultPanel, SearchResultsPanel, TextInput}
import views.html.search_page
import scala.concurrent.duration._

class search_pageSpec extends BaseUnitSpec with SearchFixture {

  private lazy val searchPage = new search_page(
    layout,
    new FormWithCSRF,
    new Paragraph,
    new TextInput(new GovukInput(new GovukErrorMessage, new GovukHint, new GovukLabel)),
    new Button(new GovukButton),
    new SearchResultsPanel(new SearchResultPanel(new Paragraph))
  )

  private def notFoundPanelIsDisplayed(doc: Document): Boolean = Option(doc.getElementById("notFound")).isDefined
  private def resultsFoundPanelIsDisplayed(doc: Document): Boolean = Option(doc.getElementById("resultsFound")).isDefined

  val searchKeyValue = "searchKeyValue"

  indexedNotableEvents.foreach{ case (notableEvent: NotableEvent, _ ) =>
    val searchKeyName = notableEvent.searchKeys.head.name
    val searchPageHeadingText = s"Search for ${notableEvent.displayName}s"
    val searchResultsTitleText = s"Results - $searchPageHeadingText"

    val formJson =
      s"""{"searchKeyName_0": "$searchKeyName", "searchKeyValue_0": "$searchKeyValue", "notableEventType": "${notableEvent.name}"}"""

    val boundForm = searchForm.bind(parse(formJson), Int.MaxValue)

    def ensureThePageIsRendered(doc: Document, titleText: String) = {
      ensureCommonPageElementsAreRendered(
        doc = doc,
        headerText = searchPageHeadingText,
        titleText = titleText,
        maybeBackLinkCall = Some(controllers.routes.SelectorController.showSelectorPage))

      val notableEventTypeElement: Elements = elementByName(doc, "notableEventType")
      val searchKeyNameElement: Elements = elementByName(doc, "searchKeyName_0")
      val searchKeyInput: Element =  doc.getElementById("searchKeyValue_0")
      val searchButton = elementByName(doc, "searchButton")

      notableEventTypeElement.`val`() mustBe notableEvent.name
      notableEventTypeElement.attr("type") mustBe "hidden"

      searchKeyNameElement.`val`() mustBe searchKeyName
      searchKeyNameElement.attr("type") mustBe "hidden"

      searchKeyInput.`val`() mustBe searchKeyValue
      searchKeyInput.attr("type") mustBe "text"

      searchButton.attr("type") mustBe "submit"
      searchButton.text() mustBe "Search"
    }

    s"the search page for notableEventType [${notableEvent.name}]" should {
      "render correctly" when {
        "no search was made" in new ViewSpec {
          override val view: HtmlFormat.Appendable = searchPage(boundForm, someUser, None, 5.minutes)

          ensureThePageIsRendered(doc, searchPageHeadingText)
          notFoundPanelIsDisplayed(doc) mustBe false
          resultsFoundPanelIsDisplayed(doc) mustBe false
        }

        "search results were not found" in new ViewSpec {
          override val view: HtmlFormat.Appendable = searchPage(boundForm, someUser, Some(Seq.empty[SearchResult]), 5.minutes)

          ensureThePageIsRendered(doc, searchResultsTitleText)
          notFoundPanelIsDisplayed(doc) mustBe true
          resultsFoundPanelIsDisplayed(doc) mustBe false
        }

        "search results were found" in new ViewSpec {
          override val view: HtmlFormat.Appendable = searchPage(boundForm, someUser, Some(Seq(vatSearchResult)), 5.minutes)

          ensureThePageIsRendered(doc, searchResultsTitleText)
          notFoundPanelIsDisplayed(doc) mustBe false
          resultsFoundPanelIsDisplayed(doc) mustBe true
        }
      }
    }
  }
}