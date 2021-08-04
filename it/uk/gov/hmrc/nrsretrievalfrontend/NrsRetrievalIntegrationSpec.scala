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

import uk.gov.hmrc.nrsretrievalfrontend.stubs.NrsRetrievalStubs._

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class NrsRetrievalIntegrationSpec extends IntegrationSpec {
  "GET /download/:vaultId/:archiveId" should {
    "pass the X-API-HEADER to the nrs-retrieval backend and return a zip file and response headers to the consumer" in {
      givenGetSubmissionBundlesReturns(OK)
      val response = wsClient.url(s"$serviceRoot/download/$vaultName/$archiveId").get.futureValue
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

      verifyGetSubmissionBundlesWithXApiKeyHeader()

      zipInputStream.close()
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
