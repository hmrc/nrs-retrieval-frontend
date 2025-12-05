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

package uk.gov.hmrc.nrsretrievalfrontend.config

import uk.gov.hmrc.nrsretrievalfrontend.support.BaseUnitSpec

class AppConfigSpec extends BaseUnitSpec:
  "appConfig" should {
    "specify the correct notable event types" in {
      appConfig.notableEvents.keySet shouldBe Set(
        "ppt-subscription",
        "ppt-return",
        "itsa-annual-adjustment",
        "itsa-crystallisation",
        "itsa-personal-income-submission",
        "itsa-ad-hoc-refund",
        "vat-registration",
        "interest-restriction-return",
        "vat-return-ui",
        "itsa-eops",
        "entry-declaration",
        "trs-registration",
        "trs-update-taxable",
        "trs-update-non-taxable",
        "vat-return",
        "saa-report-generated",
        "saa-report-acknowledged",
        "ecl-registration",
        "ecl-return",
        "ecl-amend-return",
        "ecl-amend-registration",
        "emcs-create-a-movement-ui",
        "emcs-change-a-destination-ui",
        "emcs-cancel-a-movement-ui",
        "emcs-explain-a-delay-ui",
        "emcs-explain-a-shortage-ui",
        "emcs-report-a-receipt-ui",
        "emcs-submit-alert-or-rejection-ui",
        "emcs-create-a-movement-api",
        "emcs-change-a-destination-api",
        "emcs-cancel-a-movement-api",
        "emcs-explain-a-delay-api",
        "emcs-explain-a-shortage-api",
        "emcs-report-a-receipt-api",
        "emcs-submit-alert-or-rejection-api",
        "p800refunds-nonloggedin-cheque-claim-attempt-made",
        "p800refunds-nonloggedin-bank-claim-attempt-made",
        "income-tax-view-change-adjust-payment-on-account"
      )
    }

    "specify the correct notable event for using multipleSearch" in {
      appConfig.notableEvents.filter(_._2.metadataSearchKeys).keySet shouldBe Set(
        "itsa-ad-hoc-refund",
        "income-tax-view-change-adjust-payment-on-account"
      )
    }
  }
