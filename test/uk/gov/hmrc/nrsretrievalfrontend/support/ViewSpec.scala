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

package uk.gov.hmrc.nrsretrievalfrontend.support

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers._
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.nrsretrievalfrontend.models.AuthorisedUser

trait ViewSpec extends BaseUnitSpec with Views with ViewsSelectors:

  def view: HtmlFormat.Appendable

  lazy val html: String          = view.body
  lazy val doc: Document         = Jsoup.parse(html)
  lazy val form: Element         = doc.getElementsByTag("form").first()
  lazy val heading: Element      = doc.getElementsByTag("h1").first()
  lazy val subHeading: Element   = doc.getElementsByClass("heading-secondary").first()
  lazy val errorSummary: Element = doc.getElementsByClass("amls-error-summary").first()

object ViewSpec extends ViewsSelectors:
  def elementByName(doc: Document, name: String): Elements = doc.getElementsByAttributeValue("name", name) // .first()

  val someUser: Option[AuthorisedUser] = Some(AuthorisedUser("authProviderId"))

  def ensureCommonPageElementsAreRendered(
    doc: Document,
    headerText: String,
    titleText: String,
    maybeBackLinkCall: Option[Call] = None
  ): Assertion =
    maybeBackLinkCall.map { call =>
      val backLink = doc.getElementsByClass("govuk-back-link")
      backLink.attr("href") mustBe call.url
      backLink.text() mustBe "Back"
    }

    val pageHeader: Element = doc.select(headingCssSelector).first()
    pageHeader.text() mustBe headerText
    doc.title mustBe titleText
