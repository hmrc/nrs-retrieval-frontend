/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.nrsretrievalfrontend.views.helpers

import uk.gov.hmrc.nrsretrievalfrontend.models.{Query, SearchQueries}
import uk.gov.hmrc.nrsretrievalfrontend.support.BaseUnitSpec
import uk.gov.hmrc.nrsretrievalfrontend.views.helpers.Utils.SearchMatchResult
import uk.gov.hmrc.nrsretrievalfrontend.controllers.FormMappings

class UtilsSpec extends BaseUnitSpec:
  "formatResults" should:
    "when empty" in:
      Utils.formatResults(Map.empty[String, String], Map.empty[String, String], None ) shouldBe Seq.empty[SearchMatchResult]

    "with single match" in:
      val keys = Map( "nino" -> "NINO")
      val matches = Map( "nino" -> "ABC123")
      val form = FormMappings.form.fill(SearchQueries(List[Query](Query("nino", "ABC123")  )))
      val result = Utils.formatResults(keys, matches, Some(form) )
      result shouldBe Seq[SearchMatchResult](SearchMatchResult("NINO", "ABC123", true))

    "with single no match" in:
      val keys = Map( "nino" -> "NINO")
      val matches = Map( "nino" -> "ABC123")
      val form = FormMappings.form.fill(SearchQueries(List[Query](Query("nino", "ZYX987")  )))
      val result = Utils.formatResults(keys, matches, Some(form) )
      result shouldBe Seq[SearchMatchResult](SearchMatchResult("NINO", "ABC123", false))

    "with multi item" in :
      val keys = Map("nino" -> "NINO", "utr" -> "UTR")
      val matches = Map("nino" -> "ABC123", "utr" -> "987")
      val form = FormMappings.form.fill(SearchQueries(List[Query](Query("nino", "ABC123"), Query("utr", "123"))))
      val result = Utils.formatResults(keys, matches, Some(form))
      result shouldBe Seq[SearchMatchResult](SearchMatchResult("NINO", "ABC123", true), SearchMatchResult("UTR", "987", false))


