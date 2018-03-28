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
import uk.gov.hmrc.http.HeaderCarrier
import config.{AppConfig, WSHttpT}
import models.NrsSearchResult
import support.fixtures.NrsSearchFixture
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class NrsRetrievalConnectorSpec extends UnitSpec with MockitoSugar with NrsSearchFixture {

  "search" should {
    "make a get call to /submission-metadata" in {
      when(mockWsHttp.GET[Seq[NrsSearchResult]](any())(any(), any(), any())).thenReturn(Future.successful(Seq(nrsSearchResult)))
      await(connector.search("someValue")).size shouldBe 1
    }
  }

  "submitRetrievalRequest" should {
    "make a post call to /retrieval-requests" in {
      when(mockWsHttp.doPostString(any(), any(), any())(any())).thenReturn(Future.successful(mockHttpResponse))
      await(connector.submitRetrievalRequest(1, 2)).body should be("Some Text")
    }
  }

  "statusSubmissionBundle" should {
    "make a head call to /submission-bundles" in {
      when(mockWsHttp.doHead(any())(any())).thenReturn(Future.successful(mockHttpResponse))
      await(connector.submitRetrievalRequest(1, 2)).body should be ("Some Text")
    }
  }

  "getSubmissionBundle" should {
    "make a get call to /submission-bundles" in {
      when(mockWsHttp.doGet(any())(any())).thenReturn(Future.successful(mockHttpResponse))
      await(connector.submitRetrievalRequest(1, 2)).body should be ("Some Text")
    }
  }


  private val mockWsHttp = mock[WSHttpT]
  private val mockEnvironemnt = mock[Environment]
  private val mockAppConfig = mock[AppConfig]
  private val mockHttpResponse = mock[HttpResponse]
  when(mockHttpResponse.body).thenReturn("Some Text")

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
