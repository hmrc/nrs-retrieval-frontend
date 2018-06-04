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

package model

import support.fixtures.{NrsSearchFixture, SearchFixture}
import uk.gov.hmrc.play.test.UnitSpec

class SearchTest extends UnitSpec with SearchFixture with NrsSearchFixture {

  "fromNrsSearchResult" should {
    "create a SearchResult from an NrsSearchResult based on notable event config " in {
      searchResultUtils.fromNrsSearchResult(nrsVatSearchResult) shouldBe(vatSearchResult)
      searchResultUtils.fromNrsSearchResult(nrsCdsSearchResult) shouldBe(cdsSearchResult)
    }
  }

  "searchResult" should {
    "create link text" in {
      vatSearchResult.linkText shouldBe "VAT return submitted 18 January 1970"
    }
  }

}
