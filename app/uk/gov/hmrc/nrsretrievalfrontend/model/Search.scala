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

package uk.gov.hmrc.nrsretrievalfrontend.model

import java.time.{LocalDate, ZonedDateTime}

import play.api.libs.json.{Json, OFormat}

case class SearchQuery(searchText: String)

object SearchQuery {
  implicit val formats: OFormat[SearchQuery] = Json.format[SearchQuery]
}

// todo : where does the entity name come from?
// todo : the search term(s) probably come from the search query

case class SearchResult(
  retrievalLink: RetrievalLink,
  fileName: String,
  submissionDate: ZonedDateTime) // returned in whatever format we need and presented in UTC

// todo : where do the file details come from
object SearchResult {
  def fromNrsSearchResult(nrsSearchResult: NrsSearchResult): SearchResult =
    SearchResult(
      RetrievalLink(
        nrsSearchResult.notableEvent,
        nrsSearchResult.searchKeys.taxPeriodEndDate,
        "ZIP",
        10000
      ),
      "filename",
      nrsSearchResult.userSubmissionTimestamp
    )

  implicit val formats: OFormat[SearchResult] = Json.format[SearchResult]
}

case class RetrievalLink(
  notableEventType: String,
  taxPeriodEndDate: LocalDate,
  fileType: String,
  fileSizeInBytes: Int) {
  val linkText: String = s"$notableEventType $taxPeriodEndDate $fileType $fileSizeInBytes"
}

object RetrievalLink {
  implicit val formats: OFormat[RetrievalLink] = Json.format[RetrievalLink]
}
