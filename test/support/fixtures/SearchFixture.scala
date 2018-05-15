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

package support.fixtures

import models.SearchResult
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.{JsValue, Json}

trait SearchFixture extends NrSubmissionId {

  val searchFormJson: JsValue = Json.parse("""{"searchText":"aVal"}""")

  val fileSize = 123456L
  val retrievalLink: String = SearchResult.retrievalLinkText("notableEvent", Some(LocalDate.parse("2015-11-01")), "zip", fileSize)
  val searchResult = SearchResult(retrievalLink, s"$nrSubmissionId.zip", "12345", "1234567890", 1521114973625L, None)


}
