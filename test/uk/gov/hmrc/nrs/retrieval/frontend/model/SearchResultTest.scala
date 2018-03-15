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

package uk.gov.hmrc.nrs.retrieval.frontend.model

import uk.gov.hmrc.nrs.retrieval.frontend.support.fixtures.{NrsSearchFixture, SearchFixture}
import uk.gov.hmrc.play.test.UnitSpec

class SearchResultTest extends UnitSpec with SearchFixture with NrsSearchFixture {

  "fromNrsSearchResult" should {
    "create a SearchResult from an NrsSearchResult" in {
      SearchResult.fromNrsSearchResult(nrsSearchResult) shouldBe(searchResult)
    }
  }
}
