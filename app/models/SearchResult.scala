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

import java.time.{LocalDate, ZonedDateTime}

import org.apache.commons.io.FileUtils.byteCountToDisplaySize
import play.api.libs.json.{Json, OFormat}

case class SearchResult(
  companyName: Option[String],
  retrievalLink: String,
  fileName: String,
  vaultId: Long,
  archiveId: Long,
  submissionDate: ZonedDateTime)

object SearchResult {
  def fromNrsSearchResult(nrsSearchResult: NrsSearchResult): SearchResult =
    SearchResult(
      nrsSearchResult.searchKeys.companyName,
      retrievalLinkText(
        nrsSearchResult.notableEvent,
        nrsSearchResult.searchKeys.taxPeriodEndDate,
        nrsSearchResult.bundle.fileType,
        fileSize(nrsSearchResult.bundle.fileSize)
      ),
      s"${nrsSearchResult.nrSubmissionId}.${nrsSearchResult.bundle.fileType}",
      nrsSearchResult.glacier.vaultId.toLong,
      nrsSearchResult.glacier.archiveId.toLong,
      nrsSearchResult.userSubmissionTimestamp
    )

  def fileSize(fileSizeInBytes: String): Option[Long] = {
    try {
      Some(fileSizeInBytes.toLong)
    } catch {
      case e: Exception => None
    }
  }

  def retrievalLinkText(notableEventType: String, taxPeriodEndDate: Option[LocalDate], fileType: String,
    fileSizeInBytes: Option[Long]): String = {
    Seq(Some("Retrieve"),
      Some(notableEventType),
      taxPeriodEndDate,
      Some(s".$fileType,"),
      fileSizeInBytes map (byteCountToDisplaySize(_))
    ).flatten.mkString(" ")
  }


  implicit val formats: OFormat[SearchResult] = Json.format[SearchResult]
}