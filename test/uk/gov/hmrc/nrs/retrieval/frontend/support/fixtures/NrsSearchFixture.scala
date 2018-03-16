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

package uk.gov.hmrc.nrs.retrieval.frontend.support.fixtures

import java.time.{LocalDate, ZonedDateTime}

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.nrs.retrieval.frontend.model._

trait NrsSearchFixture {

  val searchKeys = SearchKeys("vrn", Some(LocalDate.parse("2015-11-01")), Some("companyName"))

  val headerData = HeaderData("govClientPublicIP", "govClientPublicPort")

  val bundle = Bundle("zip", "123456")

  val glacier = Glacier("12345", "1234567890")

  val nrsSearchResult = NrsSearchResult("businessId", "notableEvent", "payloadContentType",
    ZonedDateTime.parse("2018-03-15T11:56:13.625Z"), Json.parse("{}"), "userAuthToken", headerData, searchKeys,
    "1234567890abcd", bundle, LocalDate.parse("2018-03-15"), glacier)

}
