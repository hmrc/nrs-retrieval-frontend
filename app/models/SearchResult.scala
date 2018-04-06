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

package models


import org.apache.commons.io.FileUtils.byteCountToDisplaySize
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.{Json, OFormat}

case class SearchResult(
  retrievalLink: String,
  fileName: String,
  vaultId: String,
  archiveId: String,
  submissionDateEpochMilli: Long,
  retrievalInProgress: Boolean = false,
  retrievalSucceeded: Boolean = false,
  retrievalFailed: Boolean = false)

object SearchResult {
  def fromNrsSearchResult(nrsSearchResult: NrsSearchResult): SearchResult =
    SearchResult(
      retrievalLinkText(
        nrsSearchResult.notableEvent,
        nrsSearchResult.searchKeys.taxPeriodEndDate,
        nrsSearchResult.bundle.fileType,
        nrsSearchResult.bundle.fileSize
      ),
      s"${nrsSearchResult.nrSubmissionId}.${nrsSearchResult.bundle.fileType}",
      nrsSearchResult.glacier.vaultName,
      nrsSearchResult.glacier.archiveId,
      nrsSearchResult.userSubmissionTimestamp.toInstant.toEpochMilli
    )

  def retrievalLinkText(notableEventType: String, taxPeriodEndDate: Option[LocalDate], fileType: String,
    fileSizeInBytes: Long): String = {
    Seq(Some(notableEventType),
      taxPeriodEndDate,
      Some(s".$fileType,"),
      Some(byteCountToDisplaySize(fileSizeInBytes))
    ).flatten.mkString(" ")
  }


  implicit val formats: OFormat[SearchResult] = Json.format[SearchResult]
}