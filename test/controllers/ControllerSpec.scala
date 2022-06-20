/*
 * Copyright 2022 HM Revenue & Customs
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

import org.jsoup.Jsoup.parse
import org.jsoup.nodes.Document
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.mockito.internal.stubbing.answers.Returns
import org.mockito.stubbing.OngoingStubbing
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers.{contentAsString, status, _}
import play.api.test.FakeRequest
import support.BaseUnitSpec
import uk.gov.hmrc.auth.core.retrieve.{Credentials, EmptyRetrieval, Name}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments, retrieve}
import views.html.components.Paragraph
import views.html.error_template

import scala.concurrent.Future

trait ControllerSpec extends BaseUnitSpec {
  val getRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/")
  val emptyPostRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", "/")
  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  lazy val error_template: error_template = new error_template(
    layout,
    new Paragraph
  )

  def givenTheRequestIsAuthorised(): OngoingStubbing[Future[Nothing]] =
    when(mockAuthConnector.authorise(any(), any())(any(), any())).thenAnswer(
      new Returns(
        Future.successful(
          retrieve.~(
            retrieve.~(
              Some(Credentials("providerId", "providerType")),
              Enrolments(Set(Enrolment("nrs_digital_investigator", Seq.empty, "state")))),
            Some(Name(Some("Terry"), Some("Test")))
          )
        )
      )
    )

  def givenTheRequestIsUnauthorised(): OngoingStubbing[Future[Nothing]] =
    when(mockAuthConnector.authorise(any(), any())(any(), any()))
      .thenAnswer(new Returns(Future.successful(EmptyRetrieval)))

  def aPageShouldBeRendered(eventualResult: Future[Result], pageHeader: String): Document = {
    val content = parse(contentAsString(eventualResult))
    status(eventualResult) shouldBe OK
    content.getElementById("pageHeader").text() shouldBe pageHeader
    content
  }

  def theNotAuthorisedPageShouldBeRendered(eventualResult: Future[Result]): Document =
    aPageShouldBeRendered(eventualResult,"Not authorised" )
}
