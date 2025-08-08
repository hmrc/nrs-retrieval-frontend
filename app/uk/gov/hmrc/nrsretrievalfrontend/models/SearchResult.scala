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

package uk.gov.hmrc.nrsretrievalfrontend.models

import com.google.inject.Inject
import org.apache.commons.io.FileUtils.byteCountToDisplaySize
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.nrsretrievalfrontend.config.AppConfig

case class SearchResult(
  notableEventDisplayName: String,
  searchKeys: Map[String, String],
  fileDetails: String,
  fileSize: String,
  vaultId: String,
  archiveId: String,
  submissionDateEpochMilli: Long,
  retrievalStatus: Option[String] = None,
  hasAttachments: Boolean
):

  val linkText: String =
    s"$notableEventDisplayName submitted ${DateTimeFormat.forPattern("d MMMM YYYY").print(submissionDateEpochMilli)}"

object SearchResult:
  given OFormat[SearchResult] = Json.format[SearchResult]

class SearchResultUtils @Inject() (appConfig: AppConfig):

  def fromNrsSearchResult(nrsSearchResult: NrsSearchResult): SearchResult =
    SearchResult(
      notableEventDisplayName = appConfig.notableEvents(nrsSearchResult.notableEvent).displayName,
      searchKeys = nrsSearchResult.searchKeys,
      fileDetails = filename(
        nrsSearchResult.nrSubmissionId,
        nrsSearchResult.bundle.fileType
      ),
      fileSize = byteCountToDisplaySize(nrsSearchResult.bundle.fileSize),
      vaultId = nrsSearchResult.glacier.vaultName,
      archiveId = nrsSearchResult.glacier.archiveId,
      submissionDateEpochMilli = nrsSearchResult.userSubmissionTimestamp.toInstant.toEpochMilli,
      retrievalStatus = None,
      hasAttachments = nrsSearchResult.attachmentIds.getOrElse(Nil).nonEmpty
    )

  private def filename(
                        nrSubmissionId: String,
                        fileType: String
                      ) =
    s"$nrSubmissionId.$fileType"
//    s"$nrSubmissionId.$fileType (${byteCountToDisplaySize(fileSize)})"
