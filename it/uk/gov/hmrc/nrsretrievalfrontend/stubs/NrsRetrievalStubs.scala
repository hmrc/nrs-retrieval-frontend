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
package uk.gov.hmrc.nrsretrievalfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.{NrsSearchResult, SearchQuery}
import play.api.libs.json.Json.toJson

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.zip.{ZipEntry, ZipOutputStream}

object NrsRetrievalStubs {
  val vaultName = "vaultName"
  val archiveId = "archiveId"
  val vatReturn = "vat-return"
  val vrn = "vrn"
  val validVrn = "validVrn"

  val notableEventType = "notableEventType"
  val searchKeyName = "searchKeyName_0"
  val searchKeyValue = "searchKeyValue_0"

  val submissionId = "604958ae-973a-4554-9e4b-fed3025dd845"
  val datetime = "Tue, 13 Jul 2021 12:36:51 GMT"

  val searchQuery: SearchQuery = SearchQuery(Some(vrn), Some(validVrn), vatReturn)

  private val xApiKeyHeader = "X-API-Key"
  private val xApiKey = "validKey"
  private val retrievalPath = "/nrs-retrieval"
  private val searchPath = s"$retrievalPath/submission-metadata?${searchQuery.searchText}"
  private val submissionBundlesPath = s"$retrievalPath/submission-bundles/$vaultName/$archiveId"
  private val submissionBundlesRetrievalRequestsPath = s"$submissionBundlesPath/retrieval-requests"
  private val equalToXApiKey = new EqualToPattern(xApiKey)

  def givenSearchReturns(status: Int, results: Seq[NrsSearchResult]): StubMapping =
    stubFor(get(urlEqualTo(s"$retrievalPath/submission-metadata?${searchQuery.searchText}"))
      .withHeader(xApiKeyHeader, equalToXApiKey)
      .willReturn(aResponse().withStatus(status).withBody(toJson(results).toString())))

  def givenSearchReturns(status: Int): StubMapping =
    stubFor(get(urlEqualTo(searchPath)).withHeader(xApiKeyHeader, equalToXApiKey).willReturn(aResponse().withStatus(status)))

  def verifySearchWithXApiKeyHeader(): Unit =
    verify(getRequestedFor(urlEqualTo(searchPath)).withHeader(xApiKeyHeader, equalToXApiKey))

  def givenGetSubmissionBundlesReturns(status: Int): StubMapping = {
    val output: Array[Byte] = "text".getBytes(Charset.defaultCharset())
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val zipOutputStream: ZipOutputStream = new ZipOutputStream(byteArrayOutputStream)
    val fileNames = Seq("submission.json", "signed-submission.p7m", "metadata.json", "signed-metadata.p7m")

    fileNames.foreach { fileName =>
      val zipEntry: ZipEntry = new ZipEntry(fileName)
      zipOutputStream.putNextEntry(zipEntry)
      zipOutputStream.write(output)
      zipOutputStream.closeEntry()
    }

    val body = byteArrayOutputStream.toByteArray

    stubFor(get(urlEqualTo(submissionBundlesPath)).willReturn(aResponse()
      .withStatus(status)
      .withBody(body)
      .withHeader("Cache-Control", "no-cache,no-store,max-age=0")
      .withHeader("Content-Length", s"${body.length}")
      .withHeader("Content-Disposition", s"inline; filename=$submissionId.zip")
      .withHeader("Content-Type", "application/octet-stream")
      .withHeader("nr-submission-id", submissionId)
      .withHeader("Date","Tue, 13 Jul 2021 12:36:51 GMT")
    ))
  }

  def verifyGetSubmissionBundlesWithXApiKeyHeader(): Unit =
    verify(getRequestedFor(urlEqualTo(submissionBundlesPath)).withHeader(xApiKeyHeader, equalToXApiKey))

  def givenPostSubmissionBundlesRetrievalRequestsReturns(status: Int): StubMapping = {
    stubFor(post(urlEqualTo(submissionBundlesRetrievalRequestsPath))
      .withHeader(xApiKeyHeader, equalToXApiKey)
      .willReturn(aResponse().withStatus(status)))
  }

  def verifyPostSubmissionBundlesRetrievalRequestsWithXApiKeyHeader(): Unit =
    verify(postRequestedFor(urlEqualTo(submissionBundlesRetrievalRequestsPath)).withHeader(xApiKeyHeader, equalToXApiKey))

  def givenHeadSubmissionBundlesReturns(status: Int): StubMapping =
    stubFor(head(urlEqualTo(submissionBundlesPath))
      .withHeader(xApiKeyHeader, equalToXApiKey)
      .willReturn(aResponse().withStatus(status)))
}