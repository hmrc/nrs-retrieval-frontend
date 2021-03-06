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

package views.helpers

case class TextInputParams(
                            name: String,
                            context: Option[String] = None,
                            hiddenNested: Boolean = false,
                            value: Option[String] = None,
                            label: String,
                            hintText: Option[String] = None,
                            error: String,
                            hasErrors: Boolean = false,
                            labelAsHeading: Boolean = false
                          )
