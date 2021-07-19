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

package connectors.testonly

import akka.util.ByteString
import connectors.NrsRetrievalConnector
import models.AuthorisedUser
import models.testonly.ValidateDownloadResult
import org.mockito.Mockito._
import org.mockito.internal.stubbing.answers.Returns
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.ws.WSResponse
import support.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class TestOnlyNrsRetrievalConnectorSpec extends UnitSpec with MockitoSugar with Status {
  private val nrsConnector = mock[NrsRetrievalConnector]
  private val connector = new TestOnlyNrsRetrievalConnectorImpl(nrsConnector)
  private val aVaultName = "vaultName"
  private val anArchiveId = "archiveId"
  private val user = AuthorisedUser("", "")
  private val wsResponse = mock[WSResponse]
  private val expectedResult = ValidateDownloadResult(OK, 0, Seq.empty, Seq.empty)

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "validateDownload" should {
    "delegate to the nrsConnector and transform the result" in {
      when(nrsConnector.getSubmissionBundle(aVaultName, anArchiveId, user)(headerCarrier)).thenReturn(Future successful wsResponse)
      when(wsResponse.status).thenReturn(OK)
      when(wsResponse.headers).thenAnswer(new Returns(Map.empty))
      when(wsResponse.bodyAsBytes).thenReturn(ByteString.empty)

      await(connector.validateDownload(aVaultName, anArchiveId, user )(headerCarrier)) shouldBe expectedResult

      verify(nrsConnector).getSubmissionBundle(aVaultName, anArchiveId, user)(headerCarrier)
    }
  }
}
