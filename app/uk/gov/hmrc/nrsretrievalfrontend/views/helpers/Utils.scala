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

package uk.gov.hmrc.nrsretrievalfrontend.views.helpers

import org.joda.time.{DateTimeZone, LocalDate}
import org.joda.time.format.DateTimeFormat
import play.api.data.Form
import uk.gov.hmrc.nrsretrievalfrontend.models.SearchQueries

// todo : find a better name than utils
object Utils:

  def formatDisplayDate(submissionDateEpochMilli: Long): String =
    s"${DateTimeFormat.forPattern("HH:mm:ss").withZone(DateTimeZone.UTC).print(submissionDateEpochMilli)} UTC"

  def formatStoredFromDate(storedFrom: LocalDate): String =
    s"${DateTimeFormat.forPattern("dd MMMM YYYY").print(storedFrom)}"

  case class SearchMatchResult(key:String, value:String, matched:Boolean)

  def makeKeyLowerCase(items: Map[String, String]): Map[String, String] =
    items.map(value => (value._1.toLowerCase, value._2)).withDefaultValue("")

  def formatResults(searchKeyList: Map[String, String], queryResult: Map[String, String], searchForm: Option[Form[SearchQueries]]): Seq[SearchMatchResult] =
    searchForm.fold( Seq.empty[SearchMatchResult]) { form =>
      val result = makeKeyLowerCase(queryResult)
      val searchKeys = makeKeyLowerCase(searchKeyList)

      val formItems = for {
        idx <- 0 to searchKeyList.size
        name <- form(s"queries[$idx].name").value
        value <- form(s"queries[$idx].value").value
      } yield (name.toLowerCase, value)

      formItems.map { (key, value) =>
        val bold = value.compareToIgnoreCase(result(key)) == 0
        SearchMatchResult(searchKeys(key), result(key), bold)
      }
    }
