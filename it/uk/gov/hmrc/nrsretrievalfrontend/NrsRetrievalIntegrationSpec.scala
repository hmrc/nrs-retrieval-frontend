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

import models.NrsSearchResult
import org.jsoup.Jsoup.parse
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.nrsretrievalfrontend.stubs.NrsRetrievalStubs._

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import scala.concurrent.Future

class NrsRetrievalIntegrationSpec extends IntegrationSpec {
  private val selectUrl = s"$serviceRoot/select"
  private val searchUrl = s"$serviceRoot/search"
  private val vatReturnSearchUrl = s"$searchUrl/$vatReturn"

  private val startPageHeading = "Search the Non-Repudiation Store"
  private val searchPageHeading = "Search for VAT returns"

  private def assertPageIsRendered(eventualResponse: Future[WSResponse], pageHeader: String) = {
    val response = eventualResponse.futureValue
    val document = parse(response.body)

    response.status shouldBe OK
    document.getElementById("pageHeader").text() shouldBe pageHeader

    document
  }

  "GET /nrs-retrieval/start" should {
    "display the start page" in {
      assertPageIsRendered(
        wsClient.url(s"$serviceRoot/start").get, startPageHeading)
    }
  }

  "GET /nrs-retrieval/select" should {
    "display the select page" in {
      assertPageIsRendered(
        wsClient.url(s"$serviceRoot/select").get, "What type of digital submission would you like to search for?")
    }
  }

  "POST /nrs-retrieval/select" should {
    "redirect to the search page" in {
      assertPageIsRendered(
        wsClient.url(selectUrl).post(Map[String, Seq[String]](notableEventType -> Seq(vatReturn))),
        searchPageHeading)
    }
  }

  "GET /nrs-retrieval/search" should {

    "redirect to the start page" when {
      "no notable event type is provided" in {
        assertPageIsRendered(wsClient.url(searchUrl).get, startPageHeading)
      }
    }

    "display the search page" when {
      "a notable event type is provided" in {
        assertPageIsRendered(wsClient.url(vatReturnSearchUrl).get, searchPageHeading)
      }
    }
  }

  "POST /nrs-retrieval/search" should {
    "perform a search and display the results panel" in {
      givenSearchReturns(OK, Seq.empty[NrsSearchResult])

      val document = assertPageIsRendered(
        wsClient.url(vatReturnSearchUrl).post(
          Map[String, Seq[String]](
            searchKeyName -> Seq(vrn),
            searchKeyValue -> Seq(validVrn),
            notableEventType -> Seq(vatReturn)
          )
        ),
        searchPageHeading
      )

      document.getElementById("notFound").text() shouldBe """No results found for "validVrn""""

      verifySearchWithXApiKeyHeader()
    }
  }

  "GET /nrs-retrieval/download/:vaultId/:archiveId" should {
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

  "GET /nrs-retrieval/retrieve/:vaultId/:archiveId" should {
    "pass the X-API-HEADER to the nrs-retrieval backend" in {
      givenPostSubmissionBundlesRetrievalRequestsReturns(OK)
      wsClient.url(s"$serviceRoot/retrieve/$vaultName/$archiveId").get.futureValue.status shouldBe ACCEPTED
      verifyPostSubmissionBundlesRetrievalRequestsWithXApiKeyHeader()
    }
  }
}
