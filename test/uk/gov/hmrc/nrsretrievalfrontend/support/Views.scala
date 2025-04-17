/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.govukfrontend.views.html.components.*
import uk.gov.hmrc.govukfrontend.views.html.helpers.{GovukFormGroup, GovukHintAndErrorMessage}
import uk.gov.hmrc.hmrcfrontend.config.{AccessibilityStatementConfig, AssetsConfig, TrackingConsentConfig, TudorCrownConfig}
import uk.gov.hmrc.hmrcfrontend.views.config.HmrcFooterItems
import uk.gov.hmrc.hmrcfrontend.views.html.components.{HmrcFooter, HmrcInternalHeader, HmrcPageHeading}
import uk.gov.hmrc.hmrcfrontend.views.html.helpers.{HmrcHead, HmrcScripts, HmrcStandardFooter, HmrcTrackingConsentSnippet}
import uk.gov.hmrc.nrsretrievalfrontend.views.html.components.*
import uk.gov.hmrc.nrsretrievalfrontend.views.html.testonly.{check_authorisation_page, validate_download_page}
import uk.gov.hmrc.nrsretrievalfrontend.views.html.{error_template, search_page, selector_page, start_page}

trait Views {
  this: Configs =>

  lazy val tudorCrownConfig = new TudorCrownConfig(this.configuration)
  lazy val trackingConsentConfig = new TrackingConsentConfig(this.configuration)
  lazy val hmrcTrackingConsentSnippet = new HmrcTrackingConsentSnippet(trackingConsentConfig)
  lazy val assetsConfig = new AssetsConfig
  lazy val hmrcInternalHeader = new HmrcInternalHeader(tudorCrownConfig)
  lazy val hmrcFooter = new HmrcFooter(govukFooter)
  lazy val accessibilityStatementConfig = new AccessibilityStatementConfig(this.configuration)
  lazy val hmrcFooterItems = new HmrcFooterItems(accessibilityStatementConfig)
  lazy val hmrcStandardFooter = new HmrcStandardFooter(hmrcFooter, hmrcFooterItems)
  lazy val hmrcHead = new HmrcHead(hmrcTrackingConsentSnippet, assetsConfig)
  lazy val hmrcScripts = new HmrcScripts(assetsConfig)
  lazy val defaultMainContent = new TwoThirdsMainContent()
  lazy val fixedWidthPageLayout = new FixedWidthPageLayout()
  lazy val govukBackLink = new GovukBackLink()
  lazy val govukErrorMessage = new GovukErrorMessage
  lazy val govukErrorSummary = new GovukErrorSummary

  lazy val hmrcPageHeading = new HmrcPageHeading
  lazy val govukFieldset = new GovukFieldset
  lazy val govukHint = new GovukHint
  lazy val govukLabel = new GovukLabel
  lazy val goveukFormGroup = new GovukFormGroup
  lazy val goveukHintAndErrorMessage = new GovukHintAndErrorMessage(govukHint, govukErrorMessage)
  lazy val govukTable = new GovukTable

  lazy val govukSkipLink = new GovukSkipLink
  lazy val govukFooter = new GovukFooter
  lazy val govukHeader = new GovukHeader(tudorCrownConfig)
  lazy val govukTemplate = new GovukTemplate(
    govukHeader,
    govukFooter,
    govukSkipLink,
    fixedWidthPageLayout
  )

  lazy val govukLayout = new GovukLayout(
    govukTemplate,
    govukHeader,
    govukFooter,
    govukBackLink,
    defaultMainContent,
    fixedWidthPageLayout
  )

  lazy val formWithCsrf = new FormWithCSRF

  lazy val layout = new Layout(
    govukLayout,
    hmrcInternalHeader,
    hmrcStandardFooter,
    hmrcHead,
    hmrcScripts,
    defaultMainContent,
    hmrcPageHeading,
    govukBackLink
  )

  lazy val mainTemplate = new MainTemplate(
    layout,
    defaultMainContent,
    govukBackLink
  )

  lazy val paragraph = new Paragraph
  lazy val govukInput = new GovukInput(govukLabel, goveukFormGroup, goveukHintAndErrorMessage)
  val govukRadios = new GovukRadios(govukFieldset, govukHint, govukLabel, goveukFormGroup, goveukHintAndErrorMessage)
  lazy val govukButton = new GovukButton

  lazy val error_template: error_template = new error_template(
    layout,
    paragraph
  )

  lazy val searchResultPanel = new SearchResultPanel(
    paragraph,
    govukErrorSummary
  )

  lazy val searchResultsPanel = new SearchResultsPanel(searchResultPanel, govukTable)

  lazy val startPage = new start_page(
    mainTemplate,
    paragraph,
    govukButton
  )

  lazy val selectorPage = new selector_page(
    mainTemplate,
    formWithCsrf,
    govukRadios,
    govukButton
  )

  lazy val searchPage = new search_page(
    mainTemplate,
    formWithCsrf,
    paragraph,
    govukInput,
    govukButton,
    searchResultsPanel
  )

  lazy val validateDownloadPage = new validate_download_page(
    layout,
    formWithCsrf,
    hmrcPageHeading,
    paragraph,
    govukInput,
    govukTable,
    govukButton
  )

  lazy val checkAuthorisationPage = new check_authorisation_page(
    layout,
    paragraph
  )
}
