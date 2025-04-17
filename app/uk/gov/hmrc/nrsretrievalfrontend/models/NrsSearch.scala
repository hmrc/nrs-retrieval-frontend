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

import java.time.ZonedDateTime

import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
case class Glacier (
  vaultName: String,
  archiveId: String
)

object Glacier {
  given OFormat[Glacier] = Json.format[Glacier]
}

case class Bundle (
  fileType: String,
  fileSize: Long
)

object Bundle {
  given OFormat[Bundle] = Json.format[Bundle]
}

case class NrsSearchResult(
  businessId: String,
  notableEvent: String,
  payloadContentType: String,
  userSubmissionTimestamp: ZonedDateTime,
  userAuthToken: String,
  nrSubmissionId: String,
  bundle: Bundle,
  attachmentIds: Option[List[String]],
  expiryDate: LocalDate,
  glacier: Glacier
)

case class NotableEventDisplay (
  name: String,
  displayName: String
)

object NrsSearchResult {

  given OFormat[NotableEventDisplay] = Json.format[NotableEventDisplay]
  given OFormat[NrsSearchResult] = Json.format[NrsSearchResult]
}
