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

import java.time.{LocalDate, ZonedDateTime}
import play.api.libs.json.{Json, OFormat}
import org.apache.commons.io.FileUtils.byteCountToDisplaySize

case class SearchQuery(searchText: String)

object SearchQuery {
  implicit val formats: OFormat[SearchQuery] = Json.format[SearchQuery]
}

case class SearchResult(
  companyName: Option[String],
  retrievalLink: RetrievalLink,
  fileName: String,
  submissionDate: ZonedDateTime) // returned in whatever format we need and presented in UTC

object SearchResult {
  def fromNrsSearchResult(nrsSearchResult: NrsSearchResult): SearchResult =
    SearchResult(
      nrsSearchResult.searchKeys.companyName,
      RetrievalLink(
        nrsSearchResult.notableEvent,
        nrsSearchResult.searchKeys.taxPeriodEndDate,
        nrsSearchResult.bundle.fileType,
        nrsSearchResult.bundle.fileSize.toInt // todo : handle the potential error here if ile size is not an int
      ),
      s"${nrsSearchResult.nrSubmissionId}.${nrsSearchResult.bundle.fileType}",
      nrsSearchResult.userSubmissionTimestamp
    )

  implicit val formats: OFormat[SearchResult] = Json.format[SearchResult]
}

case class RetrievalLink(
  notableEventType: String,
  taxPeriodEndDate: Option[LocalDate],
  fileType: String,
  fileSizeInBytes: Long) {

  val linkText: String =
    if (taxPeriodEndDate.isDefined) {
      s"Retrieve $notableEventType ${taxPeriodEndDate.get} .$fileType, ${byteCountToDisplaySize(fileSizeInBytes)}"
    } else {
      s"Retrieve $notableEventType .$fileType, ${byteCountToDisplaySize(fileSizeInBytes)}"
    }
}

object RetrievalLink {
  implicit val formats: OFormat[RetrievalLink] = Json.format[RetrievalLink]
}

case class SearchResults (results: Seq[SearchResult]) {
  def companyName: Option[String] = results flatMap (_.companyName) headOption
}