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

package support

import connectors.NrsRetrievalConnector
import models.NotableEvent
import org.scalatest.Suite
import org.scalatest.concurrent.PatienceConfiguration
import play.api.mvc.Request
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.html.components._
import uk.gov.hmrc.hmrcfrontend.config.{AccessibilityStatementConfig, AssetsConfig, TrackingConsentConfig}
import uk.gov.hmrc.hmrcfrontend.views.config.HmrcFooterItems
import uk.gov.hmrc.hmrcfrontend.views.html.components.{HmrcFooter, HmrcInternalHeader}
import uk.gov.hmrc.hmrcfrontend.views.html.helpers.{HmrcHead, HmrcScripts, HmrcStandardFooter, HmrcTrackingConsentSnippet}
import views.html.components.{Heading1, Layout}

import scala.collection.immutable

class BaseUnitSpec extends UnitSpec with StubMessageControllerComponents with PatienceConfiguration { this: Suite =>
  val nrsRetrievalConnector: NrsRetrievalConnector = mock[NrsRetrievalConnector]

  lazy val indexedNotableEvents: immutable.Seq[(NotableEvent, Int)] =
    appConfig.notableEvents.values.toList.sortBy(_.displayName).zipWithIndex

  def addToken[T](fakeRequest: FakeRequest[T]): Request[T] = {

    fakeRequest.withCSRFToken
  }

  val layout = new Layout(
    new GovukLayout(
      new GovukTemplate(
        new GovukHeader,
        new GovukFooter,
        new GovukSkipLink,
        new FixedWidthPageLayout
      ),
      new GovukHeader,
      new GovukFooter,
      new GovukBackLink,
      new TwoThirdsMainContent,
      new FixedWidthPageLayout
    ),
    new HmrcInternalHeader(),
    new HmrcStandardFooter(new HmrcFooter, new HmrcFooterItems(new AccessibilityStatementConfig(configuration))),
    new HmrcHead(new HmrcTrackingConsentSnippet(new TrackingConsentConfig(configuration)), new AssetsConfig),
    new HmrcScripts(new AssetsConfig),
    new TwoThirdsMainContent(),
    new Heading1(),
    new GovukBackLink()
  )
}