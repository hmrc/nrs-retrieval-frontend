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

package uk.gov.hmrc.nrsretrievalfrontend.models

case class Query(name: String, value: String)

object Query {

  def unapply(q: Query): Option[(String, String)] = Some((q.name, q.value))
  def queryParams(notableEvent: String, queries: List[Query], crossKeySearch: Boolean): Seq[(String, String)] = Seq(
    Seq("notableEvent" -> notableEvent),
    queries.map(q => q.name -> q.value),
    Seq("crossKeySearch" -> "true").filter(_ => crossKeySearch)
  ).flatten
}

