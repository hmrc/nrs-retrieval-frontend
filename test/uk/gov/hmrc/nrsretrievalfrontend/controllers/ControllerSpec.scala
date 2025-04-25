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

import org.jsoup.Jsoup.parse
import org.jsoup.nodes.Document
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.nrsretrievalfrontend.actions.requests.AuthenticatedRequest
import uk.gov.hmrc.nrsretrievalfrontend.actions.{AuthenticatedAction, NotableEventRefiner}
import uk.gov.hmrc.nrsretrievalfrontend.support.{BaseUnitSpec, Views, ViewsSelectors}

import scala.concurrent.Future

trait ControllerSpec extends BaseUnitSpec, Views, ViewsSelectors:
  val getRequest: FakeRequest[AnyContentAsEmpty.type]       = FakeRequest("GET", "/")
  val emptyPostRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", "/")

  def aPageShouldBeRendered(eventualResult: Future[Result], pageHeader: String): Document =
    val content = parse(contentAsString(eventualResult))
    status(eventualResult)                    shouldBe OK
    content.select(headingCssSelector).text() shouldBe pageHeader
    content

  def theNotAuthorisedPageShouldBeRendered(eventualResult: Future[Result]): Document =
    aPageShouldBeRendered(eventualResult, "Not authorised")

  val authenticatedAction = new AuthenticatedAction(
    mock[PlayAuthConnector],
    configuration,
    environment,
    stubMessagesControllerComponents(),
    error_template
  ):

    override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
      val authenticatedRequest = new AuthenticatedRequest[A]("someId", request)
      block(authenticatedRequest)

  def notableEventRefiner(notableEvent: String) = new NotableEventRefiner(
    messagesApi = messagesApi,
    errorPage = error_template
  )(notableEvent)
