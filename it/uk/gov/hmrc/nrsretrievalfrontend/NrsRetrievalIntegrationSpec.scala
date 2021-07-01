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

package uk.gov.hmrc.nrsretrievalfrontend

import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nrsretrievalfrontend.stubs.NrsRetrievalStubs._

class NrsRetrievalIntegrationSpec extends IntegrationSpec {
  private implicit val headerCarrierWithoutXApiKeyHeader: HeaderCarrier = HeaderCarrier()

  override val configuration: Map[String, Any] = defaultConfiguration + ("stride.enabled" -> false)

  private lazy val wsClient = fakeApplication().injector.instanceOf[WSClient]
  private lazy val serviceRoot = s"http://localhost:$port/nrs-retrieval"

  "GET /download/:vaultId/:archiveId" should {
    "pass the X-API-HEADER to the nrs-retrieval backend and return the file contents" when {
      "the upstream call is successful" in {
        val fileContents = "file contents"

        givenGetSubmissionBundlesReturns(OK, fileContents)

        val eventualResponse = wsClient.url(s"$serviceRoot/download/$vaultName/$archiveId").get.futureValue

        eventualResponse.status shouldBe OK
        eventualResponse.body shouldBe fileContents
        verifyGetSubmissionBundlesWithXApiKeyHeader()
      }
    }

    Seq(BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, INTERNAL_SERVER_ERROR, BAD_GATEWAY).foreach{ unexpectedStatusCode =>
      "fail" when {
        s"the upstream call returns the unexpected status code $unexpectedStatusCode" in {
          givenGetSubmissionBundlesReturns(unexpectedStatusCode)

          val eventualResponse = wsClient.url(s"$serviceRoot/download/$vaultName/$archiveId").get.futureValue

          eventualResponse.status shouldBe INTERNAL_SERVER_ERROR
          verifyGetSubmissionBundlesWithXApiKeyHeader()
        }
      }
    }
  }

  "GET /retrieve/:vaultId/:archiveId" should {
    "pass the X-API-HEADER to the nrs-retrieval backend" in {
      givenPostSubmissionBundlesRetrievalRequestsReturns(OK)
      wsClient.url(s"$serviceRoot/retrieve/$vaultName/$archiveId").get.futureValue.status shouldBe ACCEPTED
      verifyPostSubmissionBundlesRetrievalRequestsWithXApiKeyHeader()
    }
  }

  "POST /retrieve/search/:notableEventType" should {
    "pass the X-API-HEADER to the nrs-retrieval backend" in {
      givenSearchReturns(OK)

      wsClient.url(s"$serviceRoot/search/$notableEventType")
        .post(
          Map[String, Seq[String]](
            searchKeyName -> Seq(searchKeyName),
            searchKeyValue -> Seq(searchKeyValue),
            notableEventType -> Seq(notableEventType)
          )
        )
        .futureValue.status shouldBe OK

      verifySearchWithXApiKeyHeader()
    }
  }
}
