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

package uk.gov.hmrc.nrsretrievalfrontend.controllers

import play.api.data.Forms.{list, mapping, of, text}
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}
import uk.gov.hmrc.nrsretrievalfrontend.models.{Query, SearchQueries, Selector}

object FormMappings {

  val query = mapping(
      "name" -> text,
      "value" -> text
    )(Query.apply)(Query.unapply)

  val form = Form(
    mapping(
      "queries" -> list(query)
    )(SearchQueries.apply)(SearchQueries.unapply)
  )

  private val selectorFormatter: Formatter[String] = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
      data.get(key) match {
        case None | Some("") => Left(Seq(FormError(key, "search.page.error.searchkey.required")))
        case Some(s)         => Right(s)
      }

    override def unbind(key: String, value: String): Map[String, String] =
      Map(key -> value)
  }

  val selectorForm: Form[Selector] = Form(
    mapping("notableEventType" -> of(selectorFormatter))(Selector.apply)(Selector.unapply)
  )
}
