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

import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.matchers.must.Matchers.*
import play.api.libs.json.Json.parse
import play.api.mvc.AnyContentAsEmpty
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.nrsretrievalfrontend.actions.requests.NotableEventRequest
import uk.gov.hmrc.nrsretrievalfrontend.controllers.FormMappings.form
import uk.gov.hmrc.nrsretrievalfrontend.models.{NotableEvent, SearchResult}
import uk.gov.hmrc.nrsretrievalfrontend.support.ViewSpec.{elementByName, ensureCommonPageElementsAreRendered}
import uk.gov.hmrc.nrsretrievalfrontend.support.fixtures.SearchFixture
import uk.gov.hmrc.nrsretrievalfrontend.support.{BaseUnitSpec, Views, ViewSpec}

import scala.concurrent.duration.*

class metasearch_pageSpec extends BaseUnitSpec, SearchFixture, Views:

  private def notFoundPanelIsDisplayed(doc: Document): Boolean     = Option(doc.getElementById("notFound")).isDefined
  private def resultsFoundPanelIsDisplayed(doc: Document): Boolean = Option(doc.getElementById("resultsFound")).isDefined

  val searchKeyValue = "searchKeyValue"

  "check search pages with different text" should {
    val formJson = s"""{}"""
    val boundForm = form.bind(parse(formJson), Int.MaxValue)

    s"check vat-return with mandatoryText" in new ViewSpec {
      val notableEvent = indexedNotableEvents.find((n, _) => n.name == "vat-return").get._1

      given notableEventRequest: NotableEventRequest[AnyContentAsEmpty.type] =
        new NotableEventRequest(notableEvent, searchKey = notableEvent.searchKeys.head, authenticatedRequest)

      override val view: HtmlFormat.Appendable = metasearchPage(boundForm, None, 5.minutes, displayMandatoryText = true, displaySingleEntryText = false)

      doc.toString should include("Enter a VRN and an optional Period Key")
      doc.toString should include("The Period Key is to uniquely identify the return period")
    }

    s"check vat-return-ui with mandatoryText" in new ViewSpec {
      val notableEvent = indexedNotableEvents.find((n, _) => n.name == "vat-return-ui").get._1

      given notableEventRequest: NotableEventRequest[AnyContentAsEmpty.type] =
        new NotableEventRequest(notableEvent, searchKey = notableEvent.searchKeys.head, authenticatedRequest)

      override val view: HtmlFormat.Appendable = metasearchPage(boundForm, None, 5.minutes, displayMandatoryText = true, displaySingleEntryText = false)

      doc.toString should include("Enter a VRN and an optional Period Key")
      doc.toString should include("The Period Key is to uniquely identify the return period")
    }

    s"check vat-registration with mandatoryText" in new ViewSpec {
      val notableEvent = indexedNotableEvents.find((n, _) => n.name == "vat-registration").get._1

      given notableEventRequest: NotableEventRequest[AnyContentAsEmpty.type] =
        new NotableEventRequest(notableEvent, searchKey = notableEvent.searchKeys.head, authenticatedRequest)

      override val view: HtmlFormat.Appendable = metasearchPage(boundForm, None, 5.minutes, displayMandatoryText = false, displaySingleEntryText = true)

      doc.toString should include("Enter either a Post Code or Form Bundle ID")
    }
  }

  indexedNotableEvents.foreach { case (notableEvent: NotableEvent, _) =>
    val searchKeyName          = notableEvent.searchKeys.head.name
    val searchPageHeadingText  = s"Search for ${notableEvent.pluralDisplayName}"
    val searchResultsTitleText = s"Results - $searchPageHeadingText"

    val displayMandatoryText = notableEvent.searchKeys.exists(_.searchValueMandatory)
    val displaySingleEntryText = notableEvent.singleEntrySearch


    val formJson =
      s"""{"queries": [{"name": "$searchKeyName", "value": "$searchKeyValue"}]}"""

    val boundForm = form.bind(parse(formJson), Int.MaxValue)

    given notableEventRequest: NotableEventRequest[AnyContentAsEmpty.type] =
      new NotableEventRequest(notableEvent, searchKey = notableEvent.searchKeys.head, authenticatedRequest)

    def ensureThePageIsRendered(doc: Document, titleText: String) =
      ensureCommonPageElementsAreRendered(
        doc = doc,
        headerText = searchPageHeadingText,
        titleText = titleText,
        maybeBackLinkCall = Some(uk.gov.hmrc.nrsretrievalfrontend.controllers.routes.SelectorController.showSelectorPage)
      )

      val searchKeyNameElement: Elements = elementByName(doc, "queries[0].name")
      val searchKeyInput: Element        = doc.getElementById("queries[0].value")
      val searchButton                   = elementByName(doc, "searchButton")

      searchKeyNameElement.`val`() mustBe searchKeyName
      searchKeyNameElement.attr("type") mustBe "hidden"

      searchKeyInput.`val`() mustBe searchKeyValue
      searchKeyInput.attr("type") mustBe "text"

      searchButton.attr("type") mustBe "submit"
      searchButton.text() mustBe "Search"



    s"the search page for notableEventType [${notableEvent.name}]" should {
      "render correctly" when {
        "no search was made" in new ViewSpec:
          override val view: HtmlFormat.Appendable = metasearchPage(boundForm, None, 5.minutes, displayMandatoryText, displaySingleEntryText)

          ensureThePageIsRendered(doc, searchPageHeadingText)
          notFoundPanelIsDisplayed(doc) mustBe false
          resultsFoundPanelIsDisplayed(doc) mustBe false

        "search results were not found" in new ViewSpec:
          override val view: HtmlFormat.Appendable = metasearchPage(boundForm, Some(Seq.empty[SearchResult]), 5.minutes, displayMandatoryText, displaySingleEntryText)

          ensureThePageIsRendered(doc, searchResultsTitleText)
          notFoundPanelIsDisplayed(doc) mustBe true
          resultsFoundPanelIsDisplayed(doc) mustBe false

        "search results were found" in new ViewSpec:
          override val view: HtmlFormat.Appendable = metasearchPage(boundForm, Some(Seq(vatSearchResult)), 5.minutes, false, false)

          ensureThePageIsRendered(doc, searchResultsTitleText)
          notFoundPanelIsDisplayed(doc) mustBe false
          resultsFoundPanelIsDisplayed(doc) mustBe true
      }
    }
  }
