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

import play.api.libs.json._

case class SearchKeys(vrn: String, taxPeriodEndDate: LocalDate)

object SearchKeys {
  implicit val searchKeysFormat: OFormat[SearchKeys] = Json.format[SearchKeys]
}

case class HeaderData(govClientPublicIP: String, govClientPublicPort: String)

object HeaderData {
  implicit val headerDataFormat: OFormat[HeaderData] = Json.format[HeaderData]
}

case class IdentityData(internalId: String, someId: String, externalId: String, agentCode: String)

object IdentityData {
  implicit val identityDataFormat: OFormat[IdentityData] = Json.format[IdentityData]
}

case class NrsSearchResult(
  businessId: String,
  notableEvent: String,
  payloadContentType: String,
  userSubmissionTimestamp: ZonedDateTime,
  identityData: IdentityData,
  userAuthToken: String,
  headerData: HeaderData,
  searchKeys: SearchKeys,
  archiveId: String)

object NrsSearchResult {
  implicit val nrsSearchResultFormat: OFormat[NrsSearchResult] = Json.format[NrsSearchResult]
}