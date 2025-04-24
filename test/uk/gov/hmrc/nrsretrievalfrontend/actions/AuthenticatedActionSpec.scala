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

package uk.gov.hmrc.nrsretrievalfrontend.actions

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Result, Results}
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.{FakeRequest, ResultExtractors}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.nrsretrievalfrontend.actions.requests.AuthenticatedRequest
import uk.gov.hmrc.nrsretrievalfrontend.support.{BaseUnitSpec, Views}

import scala.concurrent.Future

class AuthenticatedActionSpec
    extends BaseUnitSpec with MockitoSugar with Results with Status with ScalaFutures with HeaderNames with ResultExtractors with Views:

  trait Setup:
    val mockAuthConnector = mock[AuthConnector]

    val authenticatedAction = new AuthenticatedAction(
      config = configuration,
      controllerComponents = stubMessagesControllerComponents(),
      env = environment,
      authConnector = mockAuthConnector,
      errorPage = error_template
    )

  "authenticating the request" should {
    "execute the block" when {
      "authenticated" in new Setup:
        val authProviderId = "authProviderId"

        when(mockAuthConnector.authorise[Option[Credentials]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Credentials(authProviderId, "goverment-gateway"))))

        val action: AuthenticatedRequest[_] => Future[Result] = request => Future(Ok(Json.obj("authProviderId" -> request.authProviderId)))

        val result: Future[Result] = authenticatedAction.invokeBlock(FakeRequest(), action)

        status(result) shouldBe OK
        val json: JsValue = contentAsJson(result)

        (json \ "authProviderId").as[String] shouldBe authProviderId
    }

    "redirect to login" when
      List(
        BearerTokenExpired(),
        MissingBearerToken(),
        InvalidBearerToken(),
        SessionRecordNotFound()
      ).foreach { exception =>
        s"user request failed with: ${exception.reason}" in new Setup:

          when(mockAuthConnector.authorise(any(), any())(any(), any()))
            .thenReturn(Future.failed(exception))

          val action: AuthenticatedRequest[_] => Future[Result] = request => Future(Ok("passed"))

          val result: Future[Result] = authenticatedAction.invokeBlock(FakeRequest(), action)

          status(result)           shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/stride/sign-in?successURL=http%3A%2F%2Flocalhost%2F&origin=nrs-retrieval-frontend")
      }

    "return error page" when {
      "credentials can't be found" in new Setup:

        when(mockAuthConnector.authorise[Option[Credentials]](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val action: AuthenticatedRequest[_] => Future[Result] = request => Future(Ok(Json.obj("authProviderId" -> request.authProviderId)))

        val result: Future[Result] = authenticatedAction.invokeBlock(FakeRequest(), action)

        status(result) shouldBe FORBIDDEN
        val doc: Document = Jsoup.parse(contentAsString(result))
        doc.title()                                        shouldBe "Not authorised"
        doc.select("#main-content > div > div > p").text() shouldBe "User credentials not found"

      "required enrolments are missing" in new Setup:

        when(mockAuthConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(InsufficientEnrolments()))

        val action: AuthenticatedRequest[_] => Future[Result] = request => Future(Ok(Json.obj("authProviderId" -> request.authProviderId)))

        val result: Future[Result] = authenticatedAction.invokeBlock(FakeRequest(), action)

        status(result) shouldBe FORBIDDEN
        val doc: Document = Jsoup.parse(contentAsString(result))
        doc.title()                                        shouldBe "Not authorised"
        doc.select("#main-content > div > div > p").text() shouldBe "Insufficient enrolments - Insufficient Enrolments"
    }
  }
