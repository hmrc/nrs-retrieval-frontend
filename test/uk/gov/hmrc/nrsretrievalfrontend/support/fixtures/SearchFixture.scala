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

package uk.gov.hmrc.nrsretrievalfrontend.support.fixtures

import uk.gov.hmrc.nrsretrievalfrontend.models.SearchResult

trait SearchFixture extends NrSubmissionId:
  val vatSearchResult: SearchResult =
    SearchResult("VAT return", Map.empty[String, String], s"$nrSubmissionId.zip", "120 KB", "12345", "1234567890", 1511773625L, None, false)
