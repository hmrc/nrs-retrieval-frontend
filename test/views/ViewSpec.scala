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

import models.AuthorisedUser
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers._
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import support.GuiceAppSpec

trait ViewSpec extends GuiceAppSpec {
  implicit val requestWithToken: FakeRequest[AnyContentAsEmpty.type] = addToken(FakeRequest())

  def view: HtmlFormat.Appendable

  lazy val html: String = view.body
  lazy val doc: Document = Jsoup.parse(html)
  lazy val form: Element = doc.getElementsByTag("form").first()
  lazy val heading: Element = doc.getElementsByTag("h1").first()
  lazy val subHeading: Element = doc.getElementsByClass("heading-secondary").first()
  lazy val errorSummary: Element = doc.getElementsByClass("amls-error-summary").first()
}

object ViewSpec {
  def elementByName(doc: Document, name: String): Elements = doc.getElementsByAttributeValue("name", name)//.first()

  val userName = "userName"
  val someUser: Option[AuthorisedUser] = Some(AuthorisedUser(userName, "authProviderId"))

  def ensureCommonPageElementsAreRendered(doc: Document,
                                          headerText: String,
                                          titleText: String,
                                          maybeBackLinkCall: Option[Call] = None,
                                          maybeUserName: Option[String] = Some(userName)): Assertion = {
    maybeBackLinkCall.map{ call =>
      val backLink = doc.getElementsByClass("govuk-back-link")
      backLink.attr("href") mustBe call.url
      backLink.text() mustBe "Back"
    }

    maybeUserName.map{ userName =>
      doc.getElementById("username").text() mustBe s"Welcome $userName"
    }

    val pageHeader: Element = doc.getElementById("pageHeader")
    pageHeader.text() mustBe headerText
    doc.title mustBe titleText
  }
}