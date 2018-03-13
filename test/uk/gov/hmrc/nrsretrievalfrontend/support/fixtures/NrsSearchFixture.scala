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

package uk.gov.hmrc.nrsretrievalfrontend.support.fixtures

import java.time.{LocalDate, ZonedDateTime}

import uk.gov.hmrc.nrsretrievalfrontend.model.{HeaderData, IdentityData, NrsSearchResult, SearchKeys}

trait NrsSearchFixture {

  val nrsSearchResult = NrsSearchResult("businessId", "notableEvent", "payloadContentType", ZonedDateTime.now(), identityData,
    "userAuthToken", headerData, searchKeys, "archiveId")

  val searchKeys = SearchKeys("vrn", LocalDate.parse("2015-11-01"))

  val headerData = HeaderData("govClientPublicIP", "govClientPublicPort")

  val identityData = IdentityData("internalId", "someId", "externalId", "egentCode")
}
