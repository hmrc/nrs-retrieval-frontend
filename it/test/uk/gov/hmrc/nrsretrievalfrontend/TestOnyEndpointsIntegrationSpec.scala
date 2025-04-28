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

package uk.gov.hmrc.nrsretrievalfrontend

import play.api.libs.ws.DefaultBodyWritables
import play.api.libs.ws.DefaultBodyWritables.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.nrsretrievalfrontend.stubs.NrsRetrievalStubs.givenAuthenticated

import java.net.URL
import scala.concurrent.ExecutionContext

trait TestOnyEndpointsIntegrationSpec extends IntegrationSpec:
  given executionContext: ExecutionContext      = ExecutionContext.Implicits.global
  def checkAuthorisationRequest(): HttpResponse =
    val url = new URL(s"$serviceRoot/test-only/check-authorisation")
    httpClientV2
      .get(url)
      .setHeader(authenticationHeader)
      .execute[HttpResponse]
      .futureValue

  def validateDownloadGetRequest(): HttpResponse  =
    val url = new URL(s"$serviceRoot/test-only/validate-download")
    httpClientV2
      .get(url)
      .setHeader(authenticationHeader)
      .execute[HttpResponse]
      .futureValue
  def validateDownloadPostRequest(): HttpResponse =

    val body: Map[String, Seq[String]] = Map("vaultName" -> Seq("vaultName1"), "archiveId" -> Seq("archiveId1"))

    val url = new URL(s"$serviceRoot/test-only/validate-download")
    httpClientV2
      .post(url)
      .setHeader(authenticationHeader)
      .withBody(body)
      .execute[HttpResponse]
      .futureValue

  override val configuration: Map[String, Any] =
    defaultConfiguration + ("application.router" -> "testOnlyDoNotUseInAppConf.Routes")

class TestOnyEndpointsEnabledIntegrationSpec extends TestOnyEndpointsIntegrationSpec:
  private def validate(response: HttpResponse) =
    response.status                                       shouldBe OK
    response.body.contains("Test-only validate download") shouldBe true

  "GET /nrs-retrieval/test-only/check-authorisation" should {
    "display the check-authorisation page" when {
      "the default router is used" in {
        givenAuthenticated()
        checkAuthorisationRequest().body should include("Test-only check authorisation")
      }
    }
  }

  "GET /nrs-retrieval/test-only/validate-download" should {
    "display the validate-download page" when {
      "the testOnlyDoNotUseInAppConf router is used" in {
        givenAuthenticated()
        validate(validateDownloadGetRequest())
      }
    }
  }

  "POST /nrs-retrieval/test-only/validate-download" should {
    "display the validate-download page" when {
      "the default router is used" in {
        givenAuthenticated()
        validate(validateDownloadPostRequest())
      }
    }
  }

class TestOnyEndpointsDisabledIntegrationSpec extends TestOnyEndpointsIntegrationSpec:
  override val configuration: Map[String, Any] = defaultConfiguration

  "GET /nrs-retrieval/test-only/check-authorisation" should {
    "return NOT_FOUND" when {
      "the default router is used" in {
        checkAuthorisationRequest().status shouldBe NOT_FOUND
      }
    }
  }

  "GET /nrs-retrieval/test-only/validate-download" should {
    "return NOT_FOUND" when {
      "the default router is used" in {
        validateDownloadGetRequest().status shouldBe NOT_FOUND
      }
    }
  }

  "POST /nrs-retrieval/test-only/validate-download" should {
    "return NOT_FOUND" when {
      "the default router is used" in {
        validateDownloadPostRequest().status shouldBe NOT_FOUND
      }
    }
  }
