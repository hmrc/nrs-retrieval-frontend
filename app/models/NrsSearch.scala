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

import java.time.ZonedDateTime

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

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
  nrSubmissionId: String,
  bundle: Bundle,
  expiryDate: LocalDate,
  glacier: Glacier)

case class NotableEventDisplay (
  name: String,
  displayName: String
)

object NrsSearchResult {

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  val jodaDateReads: Reads[LocalDate] = Reads[LocalDate](js =>
    js.validate[String].map[LocalDate](dtString =>
      LocalDate.parse(dtString, DateTimeFormat.forPattern(dateFormat))
    )
  )

  val jodaDateWrites: Writes[LocalDate] = new Writes[LocalDate] {
    def writes(d: LocalDate): JsValue = JsString(d.toString())
  }

  implicit val userFormat: Format[LocalDate] = Format(jodaDateReads, jodaDateWrites)

  implicit val notableEventFormat: OFormat[NotableEventDisplay] = Json.format[NotableEventDisplay]
  implicit val nrsSearchResultFormat: OFormat[NrsSearchResult] = Json.format[NrsSearchResult]
}
