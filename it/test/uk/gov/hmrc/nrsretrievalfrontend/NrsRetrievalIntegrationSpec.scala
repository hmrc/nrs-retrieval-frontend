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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.util.ByteString
import org.jsoup.Jsoup.parse
import play.api.libs.ws.DefaultBodyWritables
import play.api.libs.ws.DefaultBodyWritables.*
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.readStreamHttpResponse
import uk.gov.hmrc.nrsretrievalfrontend.models.NrsSearchResult
import uk.gov.hmrc.nrsretrievalfrontend.stubs.NrsRetrievalStubs.*

import java.io.ByteArrayInputStream
import java.net.URL
import java.util.zip.ZipInputStream
import scala.concurrent.{ExecutionContext, Future}

class NrsRetrievalIntegrationSpec extends IntegrationSpec {
  private val selectUrl = s"$serviceRoot/select"
  private val searchUrl = s"$serviceRoot/search"
  private val vatReturnSearchUrl = s"$searchUrl/$vatReturn"
  private val vatRegistrationSearchUrl = s"$searchUrl/$vatRegistration"

  private val startPageHeading = "Search the Non-Repudiation Store"
  private val vatReturnSearchPageHeading = "Search for VAT returns"
  private val vatRegistrationSearchPageHeading = "Search for VAT registrations"

  given executionContext: ExecutionContext = ExecutionContext.Implicits.global

  private def assertPageIsRendered(eventualResponse: Future[HttpResponse], pageHeader: String) = {
    val response = eventualResponse.futureValue
    val document = parse(response.body)

    response.status shouldBe OK
    document.select("#main-content > div > div > header > h1").text() shouldBe pageHeader

    document
  }

  "GET /nrs-retrieval/start" should {
    "display the start page" in {
      givenAuthenticated()
      val url = new URL(s"$serviceRoot/start")
      val responseFuture: Future[HttpResponse] = httpClientV2
        .get(url)
        .setHeader(authenticationHeader)
        .execute[HttpResponse]

      val response = responseFuture

      assertPageIsRendered(response, startPageHeading)
    }
  }

  "GET /nrs-retrieval/select" should {
    "display the select page" in {
      givenAuthenticated()

      val url = new URL(s"$serviceRoot/select")
      val responseFuture: Future[HttpResponse] = httpClientV2
        .get(url)
        .setHeader(authenticationHeader)
        .execute[HttpResponse]

      val response = responseFuture

      assertPageIsRendered(response, "What type of digital submission would you like to search for?")

    }
  }

  "POST /nrs-retrieval/select" should {
    "redirect to the search page" in {
      givenAuthenticated()
      val body: Map[String, Seq[String]] = Map(notableEventType -> Seq(vatReturn))

      val url = new URL(selectUrl)
      val responseFuture: Future[HttpResponse] = httpClientV2
        .post(url)
        .setHeader(authenticationHeader)
        .withBody(body)
        .execute[HttpResponse]

      val response = responseFuture

      assertPageIsRendered(response, vatReturnSearchPageHeading)

    }
  }

  "GET /nrs-retrieval/search" should {
    "redirect to the start page" when {
      "no notable event type is provided" in {
        givenAuthenticated()

        val url = new URL(searchUrl)
        val responseFuture: Future[HttpResponse] = httpClientV2
          .get(url)
          .setHeader(authenticationHeader)
          .execute[HttpResponse]

        val response = responseFuture

        assertPageIsRendered(response, startPageHeading)

      }
    }

    "display the search page" when {
      "a vat return is provided" in {
        givenAuthenticated()

        val url = new URL(vatReturnSearchUrl)
        val responseFuture: Future[HttpResponse] = httpClientV2
          .get(url)
          .setHeader(authenticationHeader)
          .execute[HttpResponse]

        val response = responseFuture

        val document = assertPageIsRendered(response, vatReturnSearchPageHeading)

        Option(document.getElementById("vat-registration-additional-info")).isEmpty shouldBe true
      }

      "a non vat registration is provided" in {
        givenAuthenticated()

        val url = new URL(vatRegistrationSearchUrl)
        val responseFuture: Future[HttpResponse] = httpClientV2
          .get(url)
          .setHeader(authenticationHeader)
          .execute[HttpResponse]

        val response = responseFuture

        val document = assertPageIsRendered(response, vatRegistrationSearchPageHeading)

        Option(document.getElementById("vat-registration-additional-info").id()).isDefined shouldBe true
      }
    }
  }

  "POST /nrs-retrieval/search" should {
    "perform a search and display the results panel" when {
      "a standard search is made" in {
        givenAuthenticated()
        givenSearchReturns(vatReturnSearchText, OK, Seq.empty[NrsSearchResult])

        val body: Map[String, Seq[String]] = Map(
          searchKeyName -> Seq(vrn),
          searchKeyValue -> Seq(validVrn),
          notableEventType -> Seq(vatReturn)
        )

        val url = new URL(vatReturnSearchUrl)
        val responseFuture: Future[HttpResponse] = httpClientV2
          .post(url)
          .setHeader(authenticationHeader)
          .withBody(body)
          .execute[HttpResponse]

        val response = responseFuture

        val document = assertPageIsRendered(response, vatReturnSearchPageHeading)

        document.getElementById("notFound").text() shouldBe """No results found for "validVrn""""

        verifySearchWithXApiKeyHeader(vatReturnSearchText)
      }

      "a cross key search is made" in {
        givenAuthenticated()
        givenSearchReturns(vatRegistrationSearchText, OK, Seq.empty[NrsSearchResult])

        val body: Map[String, Seq[String]] = Map(
          searchKeyName -> Seq(vatRegistrationSearchKey),
          searchKeyValue -> Seq(postCode),
          notableEventType -> Seq(vatRegistration)
        )

        val url = new URL(vatRegistrationSearchUrl)
        val responseFuture: Future[HttpResponse] = httpClientV2
          .post(url)
          .setHeader(authenticationHeader)
          .withBody(body)
          .execute[HttpResponse]

        val response = responseFuture

        val document = assertPageIsRendered(response, vatRegistrationSearchPageHeading)

        document.getElementById("notFound").text() shouldBe """No results found for "aPostCode""""

        verifySearchWithXApiKeyHeader(vatRegistrationSearchText)
      }
    }
  }

  "GET /nrs-retrieval/download/:vaultId/:archiveId" should {
    "pass the X-API-HEADER to the nrs-retrieval backend and return a zip file and response headers to the consumer" in {
      givenAuthenticated()
      givenGetSubmissionBundlesReturns(OK)

      given ActorSystem = ActorSystem()

      val url = new URL(s"$serviceRoot/download/$vatReturnNotableEvent/$vatReturn/$vrn")
      val responseFuture = httpClientV2
        .get(url)
        .setHeader(authenticationHeader)
        .stream[HttpResponse]

      responseFuture.flatMap { response =>
        response.bodyAsSource.runFold(ByteString.emptyByteString)(_ ++ _).map { bytes =>

          val zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes.toArray))
          val zippedFileNames: Seq[String] = LazyList.continually(zipInputStream.getNextEntry).takeWhile(_ != null).map(_.getName)

          assert(response.status == OK)
          assert(zippedFileNames == Seq("submission.json", "signed-submission.p7m", "metadata.json", "signed-metadata.p7m"))
          assert(response.header("Cache-Control").contains("no-cache,no-store,max-age=0"))
          assert(response.header("Content-Length").contains("276"))
          assert(response.header("Content-Disposition").contains("inline; filename=604958ae-973a-4554-9e4b-fed3025dd845.zip"))
          assert(response.header("Content-Type").contains("application/octet-stream"))
          assert(response.header("nr-submission-id").contains("604958ae-973a-4554-9e4b-fed3025dd845"))
          assert(response.header("Date").contains("Tue, 13 Jul 2021 12:36:51 GMT"))

          verifyGetSubmissionBundlesWithXApiKeyHeader()

          zipInputStream.close()
        }
      }
    }
  }

  "GET /nrs-retrieval/retrieve/:vaultId/:archiveId" should {
    "pass the X-API-HEADER to the nrs-retrieval backend" in {
      givenAuthenticated()
      givenPostSubmissionBundlesRetrievalRequestsReturns(OK)

      val url = new URL(s"$serviceRoot/retrieve/$vatReturnNotableEvent/$vatReturn/$vrn")
      httpClientV2
        .get(url)
        .setHeader(authenticationHeader)
        .execute[HttpResponse]
        .futureValue.status shouldBe ACCEPTED

      verifyPostSubmissionBundlesRetrievalRequestsWithXApiKeyHeader()
    }
  }
}
