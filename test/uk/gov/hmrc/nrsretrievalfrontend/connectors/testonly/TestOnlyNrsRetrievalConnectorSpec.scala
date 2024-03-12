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

import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.internal.stubbing.answers.Returns
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.nrsretrievalfrontend.connectors.NrsRetrievalConnector
import uk.gov.hmrc.nrsretrievalfrontend.models.AuthorisedUser
import uk.gov.hmrc.nrsretrievalfrontend.models.testonly.ValidateDownloadResult
import uk.gov.hmrc.nrsretrievalfrontend.support.UnitSpec

import scala.concurrent.Future

class TestOnlyNrsRetrievalConnectorSpec extends UnitSpec {
  private val nrsConnector = mock[NrsRetrievalConnector]
  private val httpClient = mock[HttpClient]
  private val connector = new TestOnlyNrsRetrievalConnectorImpl(nrsConnector, httpClient)
  private val aVaultName = "vaultName"
  private val anArchiveId = "archiveId"
  private val user = AuthorisedUser("", "")
  private val wsResponse = mock[WSResponse]
  private val expectedResult = ValidateDownloadResult(OK, 0, Seq.empty, Seq.empty)

  "validateDownload" should {
    "delegate to the nrsConnector and transform the result" in {
      when(nrsConnector.getSubmissionBundle(aVaultName, anArchiveId)(hc, user)).thenReturn(Future successful wsResponse)
      when(wsResponse.status).thenReturn(OK)
      when(wsResponse.headers).thenAnswer(new Returns(Map.empty))
      when(wsResponse.bodyAsBytes).thenReturn(ByteString.empty)

      await(connector.validateDownload(aVaultName, anArchiveId)(hc, user)) shouldBe expectedResult

      verify(nrsConnector).getSubmissionBundle(aVaultName, anArchiveId)(hc, user)
    }
  }

  "checkAuthorisation" should {
    "delegate to the nrs-retrieval endpoint /test-only/check-authorisation" in {
      when(httpClient.GET(endsWith("/test-only/check-authorisation"), any(), any())(any(), any(), any()))
        .thenAnswer(new Returns(Future.successful(true)))

      await(connector.checkAuthorisation()) shouldBe true
    }
  }
}
