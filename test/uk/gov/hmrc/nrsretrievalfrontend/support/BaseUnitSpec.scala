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

package uk.gov.hmrc.nrsretrievalfrontend.support

import org.scalatest.Suite
import org.scalatest.concurrent.PatienceConfiguration
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.FakeRequest
import uk.gov.hmrc.nrsretrievalfrontend.actions.requests.AuthenticatedRequest
import uk.gov.hmrc.nrsretrievalfrontend.connectors.NrsRetrievalConnector
import uk.gov.hmrc.nrsretrievalfrontend.models.NotableEvent

import scala.collection.immutable

class BaseUnitSpec extends UnitSpec with StubMessageControllerComponents with PatienceConfiguration:
  this: Suite =>
  val nrsRetrievalConnector: NrsRetrievalConnector = mock[NrsRetrievalConnector]

  lazy val indexedNotableEvents: immutable.Seq[(NotableEvent, Int)] =
    appConfig.notableEvents.values.toList.sortBy(_.displayName).zipWithIndex

  val requestWithToken: Request[AnyContentAsEmpty.type] =
    addToken(FakeRequest())

  given authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    new AuthenticatedRequest("someId", requestWithToken)

  def addToken[T](fakeRequest: FakeRequest[T]): Request[T] =

    fakeRequest.withCSRFToken
