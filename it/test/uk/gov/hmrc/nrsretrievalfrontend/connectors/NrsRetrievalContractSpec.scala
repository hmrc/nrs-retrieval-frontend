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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.util.ByteString
import org.joda.time.LocalDate
import org.scalatest.Assertion
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.nrsretrievalfrontend.IntegrationSpec
import uk.gov.hmrc.nrsretrievalfrontend.models.{AuthorisedUser, Bundle, Glacier, NrsSearchResult}
import uk.gov.hmrc.nrsretrievalfrontend.stubs.NrsRetrievalStubs.*

import java.io.ByteArrayInputStream
import java.nio.charset.Charset.*
import java.time.ZonedDateTime
import java.util.zip.ZipInputStream
import scala.concurrent.{ExecutionContext, Future}
given executionContext: ExecutionContext = ExecutionContext.Implicits.global

class NrsRetrievalContractSpec extends IntegrationSpec:
  given ActorSystem                                                                                    = ActorSystem()
  given authorisedUser: AuthorisedUser                                                                 = AuthorisedUser("authProviderId")
  private def anUpstreamErrorResponseShouldBeThrownBy[T](request: () => T, statusCode: Int): Assertion =
    intercept[Exception] {
      request()
    }.getCause.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe statusCode

  private def anInternalServerErrorShouldBeThrownBy[T](request: () => T): Assertion =
    anUpstreamErrorResponseShouldBeThrownBy(request, INTERNAL_SERVER_ERROR)

  private def aBadGatewayErrorShouldBeThrownBy[T](request: () => T): Assertion =
    anUpstreamErrorResponseShouldBeThrownBy(request, BAD_GATEWAY)

  Seq(
    (vatReturn, vatReturnSearchQuery, vatReturnSearchText, false),
    (vatRegistration, vatRegistrationSearchQuery, vatRegistrationSearchText, true)
  ).foreach { case (notableEvent, query, queryText, crossKeySearch) =>

    val search: () => Seq[NrsSearchResult] = () => connector.search(notableEvent, query.queries, crossKeySearch).futureValue

    s"a ${if crossKeySearch then "cross key" else "standard"} search" should {
      "return a sequence of results" when {
        val results =
          Seq(
            NrsSearchResult(
              businessId = "businessId",
              notableEvent = "notableEvent",
              payloadContentType = "payloadContentType",
              userSubmissionTimestamp = ZonedDateTime.now(),
              Map.empty[String, String],
              userAuthToken = Some("userAuthToken"),
              nrSubmissionId = "nrSubmissionId",
              bundle = Bundle("fileType", 1),
              attachmentIds = None,
              expiryDate = LocalDate.now(),
              glacier = Glacier(vatReturn, vrn)
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
    "return OK" when {
      "the retrieval service returns OK" in {
        givenGetSubmissionBundlesReturns(OK)

        connector.getSubmissionBundle(vatReturn, vrn).flatMap { response =>
          response.bodyAsSource.runFold(ByteString.emptyByteString)(_ ++ _).map { bytes =>
            val zipInputStream               = new ZipInputStream(new ByteArrayInputStream(bytes.toArray))
            val zippedFileNames: Seq[String] = LazyList.continually(zipInputStream.getNextEntry).takeWhile(_ != null).map(_.getName)
            assert(response.status == OK)
            assert(response.header("Cache-Control").contains("no-cache,no-store,max-age=0"))
            assert(response.header("Content-Length").contains("276"))
            assert(response.header("Content-Disposition").contains("inline; filename=604958ae-973a-4554-9e4b-fed3025dd845.zip"))
            assert(response.header("Content-Type").contains("application/octet-stream"))
            assert(response.header("nr-submission-id").contains("604958ae-973a-4554-9e4b-fed3025dd845"))
            assert(response.header("Date").contains("Tue, 13 Jul 2021 12:36:51 GMT"))
            assert(zippedFileNames == Seq("submission.json", "signed-submission.p7m", "metadata.json", "signed-metadata.p7m"))

            zipInputStream.close()
          }
        }
      }
    }
  }

  "submitRetrievalRequest" should {
    val submitRetrievalRequest: () => HttpResponse = () => connector.submitRetrievalRequest(vatReturn, vrn).futureValue

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
