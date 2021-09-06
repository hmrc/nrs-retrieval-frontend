/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}

case class SearchQuery(searchKeyName_0: Option[String], searchKeyValue_0: Option[String], notableEventType: String) {
  def searchText(crossKeySearch: Boolean): String = {
    val baseSearchText = s"notableEvent=$notableEventType&${searchKeyName_0.getOrElse("")}=${searchKeyValue_0.getOrElse("")}"

    if (crossKeySearch) s"$baseSearchText&crossKeySearch=true" else baseSearchText
  }
}

object SearchQuery {
  implicit val formats: OFormat[SearchQuery] = Json.format[SearchQuery]
}