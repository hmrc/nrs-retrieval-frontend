/*
 * Copyright 2018 HM Revenue & Customs
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

package connectors

import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import com.google.inject.{AbstractModule, Guice, Injector}
import org.mockito.Matchers.any
import play.api.Environment
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import config.{AppConfig, WSHttpT}
import models.NrsSearchResult
import support.fixtures.NrsSearchFixture
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class NrsRetrievalConnectorTest extends UnitSpec with MockitoSugar with NrsSearchFixture {

  "Search" should {
    "make a call to /search" in {
      // todo : parse this from the source object rather than hard-coding the json
      val bodyJson = Json.parse("""[{"businessId":"businessId","notableEvent":"notableEvent","payloadContentType":"payloadContentType","userSubmissionTimestamp":"2018-03-13T09:52:01.749Z[Europe/London]","identityData":{"internalId":"internalId","someId":"someId","externalId":"externalId","agentCode":"egentCode"},"userAuthToken":"userAuthToken","headerData":{"govClientPublicIP":"govClientPublicIP","govClientPublicPort":"govClientPublicPort"},"searchKeys":{"vrn":"vrn","taxPeriodEndDate":"2015-11-01"},"archiveId":"archiveId"}]""")

      when(mockWsHttp.GET[Seq[NrsSearchResult]](any())(any(), any(), any())).thenReturn(
        Future.successful(Seq(nrsSearchResult)))

      await(connector.search("someValue")).size shouldBe 1
    }
  }

  private val mockWsHttp = mock[WSHttpT]
  private val mockEnvironemnt = mock[Environment]
  private val mockAppConfig = mock[AppConfig]

  implicit val mockHeaderCarrier: HeaderCarrier = mock[HeaderCarrier]

  private val testModule = new AbstractModule {
    override def configure(): Unit = {
      bind(classOf[WSHttpT]).toInstance(mockWsHttp)
      bind(classOf[Environment]).toInstance(mockEnvironemnt)
      bind(classOf[AppConfig]).toInstance(mockAppConfig)
    }
  }

  private val injector: Injector = Guice.createInjector(testModule)
  private val connector = injector.getInstance(classOf[NrsRetrievalConnector])

}
