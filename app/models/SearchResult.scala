/*
 * Copyright 2019 HM Revenue & Customs
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


import com.google.inject.Inject
import config.AppConfig
import org.apache.commons.io.FileUtils.byteCountToDisplaySize
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{Json, OFormat}

case class SearchResult( notableEventDisplayName: String,
                         fileDetails: String,
                         vaultId: String,
                         archiveId: String,
                         submissionDateEpochMilli: Long,
                         retrievalStatus: Option[String] = None) {

  val linkText: String =
    s"$notableEventDisplayName submitted ${DateTimeFormat.forPattern("d MMMM YYYY").print(submissionDateEpochMilli)}"

}

object SearchResult {
  implicit val formats: OFormat[SearchResult] = Json.format[SearchResult]
}

class SearchResultUtils @Inject()(appConfig: AppConfig) {

  def fromNrsSearchResult(nrsSearchResult: NrsSearchResult): SearchResult =
    SearchResult(
      appConfig.notableEvents(nrsSearchResult.notableEvent).displayName,
      filename(nrsSearchResult.nrSubmissionId, nrsSearchResult.bundle.fileType, nrsSearchResult.bundle.fileSize),
      nrsSearchResult.glacier.vaultName,
      nrsSearchResult.glacier.archiveId,
      nrsSearchResult.userSubmissionTimestamp.toInstant.toEpochMilli
    )

  private def filename (nrSubmissionId: String, fileType: String, fileSize: Long) =
    s"$nrSubmissionId.$fileType (${byteCountToDisplaySize(fileSize)})"

}