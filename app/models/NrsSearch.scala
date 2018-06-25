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

import java.time.ZonedDateTime

import org.joda.time.LocalDate
import play.api.libs.json._

case class SearchKeys(vrn: String, taxPeriodEndDate: Option[LocalDate])

object SearchKeys {
  implicit val searchKeysFormat: OFormat[SearchKeys] = Json.format[SearchKeys]
}

case class HeaderData(govClientPublicIP: String, govClientPublicPort: String)

case class Glacier (
  vaultName: String,
  archiveId: String
)

object Glacier {
  implicit val formats: OFormat[Glacier] = Json.format[Glacier]
}

case class Bundle (
  fileType: String,
  fileSize: Long
)

object Bundle {
  implicit val formats: OFormat[Bundle] = Json.format[Bundle]
}
case class NrsSearchResult(
  businessId: String,
  notableEvent: String,
  payloadContentType: String,
  userSubmissionTimestamp: ZonedDateTime,
  identityData: JsValue,
  userAuthToken: String,
  headerData: JsValue,
  searchKeys: SearchKeys,
  nrSubmissionId: String,
  bundle: Bundle,
  expiryDate: LocalDate,
  glacier: Glacier)

object NrsSearchResult {
  implicit val notableEventFormat: OFormat[NotableEvent] = Json.format[NotableEvent]
  implicit val nrsSearchResultFormat: OFormat[NrsSearchResult] = Json.format[NrsSearchResult]
}
