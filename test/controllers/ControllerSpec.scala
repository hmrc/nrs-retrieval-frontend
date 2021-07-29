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

package controllers

import play.api.inject.Injector
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, StubControllerComponentsFactory}
import support.GuiceAppSpec
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.error_template

trait ControllerSpec extends GuiceAppSpec with StubControllerComponentsFactory {
  val getRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/")
  val postRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", "/")
  val error_template: error_template = injector.instanceOf[error_template]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
}
