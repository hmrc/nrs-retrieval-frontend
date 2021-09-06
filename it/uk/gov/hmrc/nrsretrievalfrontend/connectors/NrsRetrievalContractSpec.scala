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

import connectors.NrsRetrievalConnectorImpl
import models._
import org.joda.time.LocalDate
import org.scalatest.Assertion
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.nrsretrievalfrontend.IntegrationSpec
import uk.gov.hmrc.nrsretrievalfrontend.stubs.NrsRetrievalStubs._

import java.io.ByteArrayInputStream
import java.time.ZonedDateTime
import java.util.zip.ZipInputStream

class NrsRetrievalContractSpec extends IntegrationSpec {
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val authorisedUser = AuthorisedUser("userName", "authProviderId")

  private lazy val connector = injector.instanceOf[NrsRetrievalConnectorImpl]

  private def anUpstreamErrorResponseShouldBeThrownBy[T](request: () => T, statusCode: Int): Assertion =
    intercept[Exception] {
      request()
    }.getCause.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe statusCode

  private def anInternalServerErrorShouldBeThrownBy[T](request: () => T): Assertion =
    anUpstreamErrorResponseShouldBeThrownBy(request, INTERNAL_SERVER_ERROR)

  private def aBadGatewayErrorShouldBeThrownBy[T](request: () => T): Assertion =
    anUpstreamErrorResponseShouldBeThrownBy(request, BAD_GATEWAY)

  val crossKeySearch = false

  Seq(
    (vatReturnSearchQuery, vatReturnSearchText, false),
    (vatRegistrationSearchQuery, vatRegistrationSearchText, true)).foreach { case (query, queryText, crossKeySearch) =>

    def search(): Seq[NrsSearchResult] = connector.search(query, authorisedUser, crossKeySearch).futureValue

    s"a ${if (crossKeySearch) "cross key" else "standard"} search" should {
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
              Bundle("fileType", 1),
              LocalDate.now(),
              Glacier(vatReturn, vrn)
            )
          )

        "the retrieval service returns OK with results" in {
          givenSearchReturns(queryText, OK, results)
          search() shouldBe results
        }

        "the retrieval service returns ACCEPTED with results" in {
          givenSearchReturns(queryText, ACCEPTED, results)
          search() shouldBe results
        }
      }

      "return an empty sequence" when {
        val emptyResults = Seq.empty

        "the retrieval service returns OK with empty results" in {
          givenSearchReturns(queryText, OK, emptyResults)
          search() shouldBe emptyResults
        }

        "the retrieval service returns ACCEPTED with empty results" in {
          givenSearchReturns(queryText, ACCEPTED, emptyResults)
          search() shouldBe emptyResults
        }

        "the retrieval service returns NOT_FOUND" in {
          givenSearchReturns(queryText, NOT_FOUND)
          search() shouldBe emptyResults
        }
      }

      "fail" when {
        "the retrieval service returns INTERNAL_SERVER_ERROR" in {
          givenSearchReturns(queryText, INTERNAL_SERVER_ERROR)
          anInternalServerErrorShouldBeThrownBy(search)
        }

        "the retrieval service returns BAD_GATEWAY" in {
          givenSearchReturns(queryText, BAD_GATEWAY)
          aBadGatewayErrorShouldBeThrownBy(search)
        }
      }
    }
  }

  "getSubmissionBundle" should {
    def submissionBundle(): WSResponse = connector.getSubmissionBundle(vatReturn, vrn, authorisedUser).futureValue

    // the backend only returns OK here, to do fix under NONPR-2082
    "return OK" when {
      "the retrieval service returns OK" in {
        givenGetSubmissionBundlesReturns(OK)

        val response = submissionBundle()
        val body = response.bodyAsBytes
        val zipInputStream = new ZipInputStream(new ByteArrayInputStream(body.toArray))
        val zippedFileNames: Seq[String] = Stream.continually(zipInputStream.getNextEntry).takeWhile(_ != null).map(_.getName)

        response.status shouldBe OK
        zippedFileNames shouldBe Seq("submission.json", "signed-submission.p7m", "metadata.json", "signed-metadata.p7m")
        response.header("Cache-Control") shouldBe Some("no-cache,no-store,max-age=0")
        response.header("Content-Length") shouldBe Some(s"${body.size}")
        response.header("Content-Disposition") shouldBe Some("inline; filename=604958ae-973a-4554-9e4b-fed3025dd845.zip")
        response.header("Content-Type") shouldBe Some("application/octet-stream")
        response.header("nr-submission-id") shouldBe Some("604958ae-973a-4554-9e4b-fed3025dd845")
        response.header("Date") shouldBe Some("Tue, 13 Jul 2021 12:36:51 GMT")

        zipInputStream.close()
      }
    }
  }

  "submitRetrievalRequest" should {
    def submitRetrievalRequest(): HttpResponse =
      connector.submitRetrievalRequest(vatReturn, vrn, authorisedUser).futureValue

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

    Seq(NOT_FOUND, INTERNAL_SERVER_ERROR, BAD_GATEWAY).foreach { status =>
      s"return $status" when {
        s"the retrieval service returns $status" in {
          givenPostSubmissionBundlesRetrievalRequestsReturns(status)
          submitRetrievalRequest().status shouldBe status
        }
      }
    }
  }

  "statusSubmissionBundle" should {
    def statusSubmissionBundle(): HttpResponse = connector.statusSubmissionBundle(vatReturn, vrn).futureValue

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

    Seq(NOT_FOUND, INTERNAL_SERVER_ERROR, BAD_GATEWAY).foreach { status =>
      s"return $status" when {
        s"the retrieval service returns $status" in {
          givenHeadSubmissionBundlesReturns(status)
          statusSubmissionBundle().status shouldBe status
        }
      }
    }
  }
}
