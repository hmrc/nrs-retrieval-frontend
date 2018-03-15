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
import play.api.libs.functional.syntax._

case class SearchKeys(vrn: String, taxPeriodEndDate: Option[LocalDate], companyName: Option[String])

object SearchKeys {
  implicit val searchKeysFormat: OFormat[SearchKeys] = Json.format[SearchKeys]
}

case class HeaderData(govClientPublicIP: String, govClientPublicPort: String)

object HeaderData {
  val headerDataReads: Reads[HeaderData] = (
    (JsPath \ "Gov-Client-Public-IP").read[String] and
      (JsPath \ "Gov-Client-Public-Port").read[String]
    )(HeaderData.apply _)

  val headerDataWrites: Writes[HeaderData] = (
    (JsPath \ "Gov-Client-Public-IP").write[String] and
      (JsPath \ "Gov-Client-Public-Port").write[String]
    )(unlift(HeaderData.unapply))

  implicit val headerDataFormat: Format[HeaderData] =
    Format(headerDataReads, headerDataWrites)
}

case class IdentityData(internalId: String, externalId: String, agentCode: String)

object IdentityData {
  implicit val identityDataFormat: OFormat[IdentityData] = Json.format[IdentityData]
}

case class Glacier (
  vaultId: String,
  archiveId: String
)

object Glacier {
  implicit val formats: OFormat[Glacier] = Json.format[Glacier]
}

case class Bundle (
  fileType: String,
  fileSize: String,
  expiryDate: ZonedDateTime
)

object Bundle {
  implicit val formats: OFormat[Bundle] = Json.format[Bundle]
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
  nrSubmissionId: String,
  bundle: Bundle,
  glacier: Glacier)

object NrsSearchResult {
  implicit val nrsSearchResultFormat: OFormat[NrsSearchResult] = Json.format[NrsSearchResult]
}
