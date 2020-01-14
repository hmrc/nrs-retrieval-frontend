/*
 * Copyright 2020 HM Revenue & Customs
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

package views.helpers

import org.joda.time.{DateTimeZone, LocalDate}
import org.joda.time.format.DateTimeFormat

// todo : find a better name than utils
object Utils {

  def formatDisplayDate (submissionDateEpochMilli: Long): String =
    s"${DateTimeFormat.forPattern("HH:mm:ss").withZone(DateTimeZone.UTC).print(submissionDateEpochMilli)} UTC"

  def formatStoredFromDate (storedFrom: LocalDate): String =
    s"${DateTimeFormat.forPattern("dd MMMM YYYY").print(storedFrom)}"

}
