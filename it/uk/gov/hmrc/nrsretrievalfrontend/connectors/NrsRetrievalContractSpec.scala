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

package uk.gov.hmrc.nrsretrievalfrontend.connectors

import com.github.tomakehurst.wiremock.client.WireMock
import connectors.NrsRetrievalConnector
import models._
import org.joda.time.LocalDate
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{Assertion, BeforeAndAfterEach, Matchers, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.baseApplicationBuilder.injector
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.nrsretrievalfrontend.stubs.NrsRetrievalStubs._
import uk.gov.hmrc.nrsretrievalfrontend.wiremock.WireMockSupport

import java.time.ZonedDateTime

class NrsRetrievalContractSpec
  extends WordSpecLike
    with Matchers
    with ScalaFutures
    with GuiceOneServerPerSuite
    with WireMockSupport
    with IntegrationPatience
    with BeforeAndAfterEach {
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val connector = injector.instanceOf[NrsRetrievalConnector]

  private val authorisedUser = AuthorisedUser("userName", "authProviderId")

  override def beforeEach(): Unit = WireMock.reset()

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(Map[String, Any](
      "microservice.services.nrs-retrieval.port" -> wireMockPort, //not effective - had to set wiremock port to configured nrs port. Wy???
      "auditing.enabled" -> false,
      "metrics.jvm" -> false)
    ).build()

  private def anUpstreamErrorResponseShouldBeThrownBy[T](request: () => T, statusCode: Int): Assertion =
    intercept[Exception] {
      request()
    }.getCause.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe statusCode

  private def anInternalServerErrorShouldBeThrownBy[T](request: () => T): Assertion =
    anUpstreamErrorResponseShouldBeThrownBy(request, INTERNAL_SERVER_ERROR)

  private def aBadGatewayErrorShouldBeThrownBy[T](request: () => T): Assertion =
    anUpstreamErrorResponseShouldBeThrownBy(request, BAD_GATEWAY)

  private def aNotFoundErrorShouldBeThrownBy[T](request: () => T): Assertion =
    intercept[Exception] {
      request()
    }.getCause.asInstanceOf[HttpException].responseCode shouldBe NOT_FOUND

  "search" should {
    val searchQuery = SearchQuery(Some("searchKeyName_0"), Some("searchKeyValue_0"), "notableEventType")

    def search(): Seq[NrsSearchResult] = connector.search(searchQuery, authorisedUser).futureValue

    "return a sequence of results" when {
      val results =
        Seq(
          NrsSearchResult(
            "businessId",
            "notableEvent",
            "payloadContentType",
            ZonedDateTime.now(),
            None,
            "userAuthToken",
            None,
            "nrSubmissionId",
            Bundle ("fileType", 1),
            LocalDate.now(),
            Glacier(vaultName, archiveId)
          )
        )

      "the retrieval service returns OK with results" in {
        givenSearchReturns(searchQuery, OK, results)
        search() shouldBe results
      }

      "the retrieval service returns ACCEPTED with results" in {
        givenSearchReturns(searchQuery, ACCEPTED, results)
        search() shouldBe results
      }
    }

    "return an empty sequence" when {
      val emptyResults = Seq.empty

      "the retrieval service returns OK with empty results" in {
        givenSearchReturns(searchQuery, OK, emptyResults)
        search() shouldBe emptyResults
      }

      "the retrieval service returns ACCEPTED with empty results" in {
        givenSearchReturns(searchQuery, ACCEPTED, emptyResults)
        search() shouldBe emptyResults
      }

      "the retrieval service returns NOT_FOUND" in {
        givenSearchReturns(searchQuery, NOT_FOUND)
        search() shouldBe emptyResults
      }
    }

    "fail" when {
      "the retrieval service returns INTERNAL_SERVER_ERROR" in {
        givenSearchReturns(searchQuery, INTERNAL_SERVER_ERROR)
        anInternalServerErrorShouldBeThrownBy(search)
      }

      "the retrieval service returns BAD_GATEWAY" in {
        givenSearchReturns(searchQuery, BAD_GATEWAY)
        aBadGatewayErrorShouldBeThrownBy(search)
      }
    }
  }

  "getSubmissionBundle" should {
    def submissionBundle(): WSResponse = connector.getSubmissionBundle(vaultName, archiveId, authorisedUser).futureValue

    // the backend only returns OK here...
    "return OK" when {
      "the retrieval service returns OK" in {
        givenGetSubmissionBundles(OK)
        submissionBundle().status shouldBe OK
      }
    }
  }

  "submitRetrievalRequest" should {
    def submitRetrievalRequest(): HttpResponse =
      connector.submitRetrievalRequest(vaultName, archiveId, authorisedUser).futureValue

    "return OK" when {
      "the retrieval service returns OK" in {
        givenPostSubmissionBundlesRetrievalRequestsReturns(OK)
        submitRetrievalRequest().status shouldBe OK
      }
    }

    "return ACCEPTED" when {
      "the retrieval service returns ACCEPTED" in {
        givenPostSubmissionBundlesRetrievalRequestsReturns(ACCEPTED)
        submitRetrievalRequest().status shouldBe ACCEPTED
      }
    }

    "fail" when {
      "the retrieval service returns NOT_FOUND" in {
        givenPostSubmissionBundlesRetrievalRequestsReturns(NOT_FOUND)
        aNotFoundErrorShouldBeThrownBy(submitRetrievalRequest)
      }

      "the retrieval service returns INTERNAL_SERVER_ERROR" in {
        givenPostSubmissionBundlesRetrievalRequestsReturns(INTERNAL_SERVER_ERROR)
        anInternalServerErrorShouldBeThrownBy(submitRetrievalRequest)
      }

      "the retrieval service returns BAD_GATEWAY" in {
        givenPostSubmissionBundlesRetrievalRequestsReturns(BAD_GATEWAY)
        aBadGatewayErrorShouldBeThrownBy(submitRetrievalRequest)
      }
    }
  }

  "statusSubmissionBundle" should {
    def statusSubmissionBundle(): HttpResponse = connector.statusSubmissionBundle(vaultName, archiveId).futureValue

    "return OK" when {
      "the retrieval service returns OK" in {
        givenHeadSubmissionBundlesReturns(OK)
        statusSubmissionBundle().status shouldBe OK
      }
    }

    "return ACCEPTED" when {
      "the retrieval service returns ACCEPTED" in {
        givenHeadSubmissionBundlesReturns(ACCEPTED)
        statusSubmissionBundle().status shouldBe ACCEPTED
      }
    }

    "fail" when {
      "the retrieval service returns NOT_FOUND" in {
        givenHeadSubmissionBundlesReturns(NOT_FOUND)
        aNotFoundErrorShouldBeThrownBy(statusSubmissionBundle)
      }

      "the retrieval service returns INTERNAL_SERVER_ERROR" in {
        givenHeadSubmissionBundlesReturns(INTERNAL_SERVER_ERROR)
        anInternalServerErrorShouldBeThrownBy(statusSubmissionBundle)
      }

      "the retrieval service returns BAD_GATEWAY" in {
        givenHeadSubmissionBundlesReturns(BAD_GATEWAY)
        aBadGatewayErrorShouldBeThrownBy(statusSubmissionBundle)
      }
    }
  }
}

