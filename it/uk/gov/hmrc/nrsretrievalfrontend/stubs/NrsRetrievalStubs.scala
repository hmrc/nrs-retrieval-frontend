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
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.{NrsSearchResult, SearchQuery}
import play.api.libs.json.Json.toJson

object NrsRetrievalStubs {
  val vaultName = "vaultName"
  val archiveId = "archiveId"

  private val retrievalPath = "/nrs-retrieval"
  private val submissionBundlesPath = s"$retrievalPath/submission-bundles/$vaultName/$archiveId"

  def givenSearchReturns(searchQuery: SearchQuery, status: Int, results: Seq[NrsSearchResult]): StubMapping =
    stubFor(get(urlEqualTo(s"$retrievalPath/submission-metadata?${searchQuery.searchText}"))
      .willReturn(aResponse().withStatus(status).withBody(toJson(results).toString())))

  def givenSearchReturns(searchQuery: SearchQuery, status: Int): StubMapping =
    stubFor(get(urlEqualTo(s"$retrievalPath/submission-metadata?${searchQuery.searchText}"))
      .willReturn(aResponse().withStatus(status)))

  def givenGetSubmissionBundles(status: Int): StubMapping =
    stubFor(get(urlEqualTo(submissionBundlesPath)).willReturn(aResponse().withStatus(status)))

  def givenPostSubmissionBundlesRetrievalRequestsReturns(status: Int): StubMapping =
    stubFor(post(urlEqualTo(s"$submissionBundlesPath/retrieval-requests"))
      .willReturn(aResponse().withStatus(status)))

  def givenHeadSubmissionBundlesReturns(status: Int): StubMapping =
    stubFor(head(urlEqualTo(submissionBundlesPath)).willReturn(aResponse().withStatus(status)))
}