/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.nrsretrievalfrontend.connectors.testonly

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.mockito.Mockito.*
import org.mockito.internal.stubbing.answers.Returns
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.nrsretrievalfrontend.connectors.NrsRetrievalConnector
import uk.gov.hmrc.nrsretrievalfrontend.models.AuthorisedUser
import uk.gov.hmrc.nrsretrievalfrontend.models.testonly.ValidateDownloadResult
import uk.gov.hmrc.nrsretrievalfrontend.support.UnitSpec

import scala.concurrent.Future

class TestOnlyNrsRetrievalConnectorSpec extends UnitSpec {
  private val nrsConnector = mock[NrsRetrievalConnector]
  private val httpClient = mock[HttpClientV2]
  private val connector = new TestOnlyNrsRetrievalConnectorImpl(nrsConnector, httpClient)
  private val aVaultName = "vaultName"
  private val anArchiveId = "archiveId"
  private val user = AuthorisedUser("", "")
  private val httpResponse = mock[HttpResponse]
  private val expectedResult = ValidateDownloadResult(OK, 0, Seq.empty, Seq.empty)

  "validateDownload" should {
    "delegate to the nrsConnector and transform the result" in {
      when(nrsConnector.getSubmissionBundle(aVaultName, anArchiveId)(using hc, user)).thenReturn(Future successful httpResponse)
      when(httpResponse.status).thenReturn(OK)
      when(httpResponse.headers).thenAnswer(new Returns(Map.empty))
      when(httpResponse.bodyAsSource).thenReturn(Source.single(ByteString.empty))

      await(connector.validateDownload(aVaultName, anArchiveId)(using hc, user)) shouldBe expectedResult

      verify(nrsConnector).getSubmissionBundle(aVaultName, anArchiveId)(using hc, user)
    }
  }

  //java.lang.NullPointerException: Cannot invoke "uk.gov.hmrc.http.client.RequestBuilder.execute(uk.gov.hmrc.http.HttpReads, scala.concurrent.ExecutionContext)" because the return value of "uk.gov.hmrc.http.client.HttpClientV2.get(java.net.URL, uk.gov.hmrc.http.HeaderCarrier)" is null
/*  "checkAuthorisation" should {
    "delegate to the nrs-retrieval endpoint /test-only/check-authorisation" in {
      val mockRequestBuilder = mock[RequestBuilder]
      val testConnector = new TestOnlyNrsRetrievalConnectorImpl(nrsConnector,httpClient)
      val testUrl = new URL("http://localhost:1234/test-only/check-authorisation")
      when(httpClient.get(eq(testUrl))(any()))
        .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute[Boolean])
          .thenReturn(Future.successful(true))

      await(testConnector.checkAuthorisation) shouldBe true
    }
  }*/
}
