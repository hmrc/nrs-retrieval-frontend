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

package uk.gov.hmrc.nrsretrievalfrontend.models

import play.api.libs.json.*

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

case class SearchKeySubmission(
  searchKeyName: String,
  searchKeyValue: Option[String]
)

case class SearchKey(
  name: String,
  label: String
)

object SearchKey:
  given OFormat[SearchKey] = Json.format[SearchKey]

case class NotableEvent(
  name: String,
  displayName: String,
  pluralDisplayName: String,
  storedFrom: String,
  storedFor: String,
  searchKeys: Seq[SearchKey],
  estimatedRetrievalTime: FiniteDuration,
  crossKeySearch: Boolean,
  metadataSearchKeys: Boolean = false
)

object NotableEvent:
  given Format[FiniteDuration] = new Format[FiniteDuration]:
    override def reads(json: JsValue): JsResult[FiniteDuration] =
      Try(Duration.apply(json.as[String])).toOption
        .collect { case duration: FiniteDuration =>
          JsSuccess(duration)
        }
        .getOrElse(
          JsError(s"can't build finite duration from string value: $json")
        )

    override def writes(o: FiniteDuration): JsValue =
      JsString(o.toString())

  given OFormat[NotableEvent] = Json.format[NotableEvent]
