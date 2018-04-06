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

import models.SearchResult
import support.fixtures.{NrsSearchFixture, SearchFixture}
import uk.gov.hmrc.play.test.UnitSpec

class SearchTest extends UnitSpec with SearchFixture with NrsSearchFixture {

  "fromNrsSearchResult" should {
    "create a SearchResult from an NrsSearchResult" in {
      SearchResult.fromNrsSearchResult(nrsSearchResult) shouldBe(searchResult)
    }
  }

  "RetrievalLink.linktext" should {
    "create link text when all values are provide" in {
      retrievalLink shouldBe "notableEvent 2015-11-01 .zip, 120 KB"
    }

    "create link text when only mandatory values are provide" in {
      val retrievalLink = SearchResult.retrievalLinkText("notableEvent", None, "zip", fileSize)
      retrievalLink shouldBe "notableEvent .zip, 120 KB"
    }
  }

}
