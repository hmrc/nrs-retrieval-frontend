/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.nrsretrievalfrontend

import uk.gov.hmrc.nrsretrievalfrontend.models.{Query, SearchQueries}

import java.net.URLEncoder

trait Fixture {
  val vatReturn = "vat-return"
  val vatReturnNotableEvent = "vat-return"
  val vrn = "vrn"
  val validVrn = "validVrn"

  val notableEventType = "notableEventType"
  val searchKeyName = "queries[0].name"
  val searchKeyValue = "queries[0].value"

  val submissionId = "604958ae-973a-4554-9e4b-fed3025dd845"
  val datetime = "Tue, 13 Jul 2021 12:36:51 GMT"

  val xApiKeyHeader = "X-API-Key"
  val xApiKey = "validKey"

  val vatRegistration = "vat-registration"
  val vatRegistrationSearchKey = "postCodeOrFormBundleId"
  val postCode = "aPostCode"

  def queryString(query: Seq[(String, String)]): String =
    query.map { case (k, v) => s"$k=${URLEncoder.encode(v, "utf-8")}" }.mkString("", "&", "")

  val vatReturnSearchQuery: SearchQueries = SearchQueries(List(Query(vrn, validVrn)))
  val vatReturnSearchText: String = queryString(Query.queryParams(vatReturn, vatReturnSearchQuery.queries, false))

  val vatRegistrationSearchQuery: SearchQueries = SearchQueries(List(Query(vatRegistrationSearchKey, postCode)))
  val vatRegistrationSearchText: String = queryString(Query.queryParams(vatRegistration, vatRegistrationSearchQuery.queries, true))
}
