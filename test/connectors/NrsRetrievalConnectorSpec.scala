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

import com.google.inject.name.Names
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import com.google.inject.{AbstractModule, Guice, Injector}
import org.mockito.Matchers.any
import play.api.Environment
import config.{AppConfig, Auditable, MicroserviceAudit, WSHttpT}
import javax.inject.Provider
import models.NrsSearchResult
import models.audit.{DataEventAuditType, NonRepudiationStoreDownload, NonRepudiationStoreRetrieve, NonRepudiationStoreSearch}
import org.scalatest.BeforeAndAfterEach
import play.api.libs.iteratee.Iteratee
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import support.fixtures.{Infrastructure, NrsSearchFixture}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit

import scala.concurrent.Future

class NrsRetrievalConnectorSpec extends UnitSpec with MockitoSugar with NrsSearchFixture with Infrastructure with BeforeAndAfterEach {

  "search" should {
    "make a get call to /submission-metadata returning data" in {
      when(mockWsHttp.GET[Seq[NrsSearchResult]](any())(any(), any(), any())).thenReturn(Future.successful(Seq(nrsSearchResult)))
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
      await(connector.search("someValue")).size shouldBe 1
      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])(any())
    }

    "make a get call to /submission-metadata with parameters returning no data" in {
      when(mockWsHttp.GET[Seq[NrsSearchResult]](any())(any(), any(), any())).thenReturn(Future.failed(new Throwable("404")))
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
      await(connector.search("someValue")).size shouldBe 0
      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])(any())
    }

    "make a get call to /submission-metadata with parameters resulting in a failure" in {
      when(mockWsHttp.GET[Seq[NrsSearchResult]](any())(any(), any(), any())).thenReturn(Future.failed(new Throwable("401")))
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
      a[Throwable] should be thrownBy await(connector.search("someValue"))
      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])(any())
    }

//    "make a get call to /submission-metadata and retrieve nr-submission-id from header" in {
//      when(mockWsHttp.GET[Seq[NrsSearchResult]](any())(any(), any(), any())).thenReturn(Future.successful(Seq(nrsSearchResult)))
//      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
//      a[Throwable] should be thrownBy await(connector.search("someValue"))
//      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])(any())
//    }
 }

  "submitRetrievalRequest" should {
    "make a post call to /retrieval-requests" in {
      when(mockWsHttp.POST[Any, Any](any(), any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(mockHttpResponse))
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
      await(connector.submitRetrievalRequest(testAuditId, testArchiveId)).body should be("Some Text")
      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreRetrieve])(any())
    }
  }

  "statusSubmissionBundle" should {
    "make a head call to /submission-bundles" in {
      when(mockWsHttp.HEAD[Any](any())(any(), any(), any())).thenReturn(Future.successful(mockHttpResponse))
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
      await(connector.statusSubmissionBundle(testAuditId, testArchiveId)).body should be ("Some Text")
      verify(mockAuditable, times(0)).sendDataEvent(any[NonRepudiationStoreDownload])(any())
    }
  }

  "getSubmissionBundle" should {
    "make a get call to /submission-bundles" in {
      when(mockWsHttp.GET[Any](any())(any(), any(), any())).thenReturn(Future.successful(mockHttpResponse))

      val mockWSRequest2 = mock[WSRequest]
      when(mockWSRequest2.get(any())(any())).thenReturn(Future.failed(new Throwable))

      val mockWSRequest1 = mock[WSRequest]
      when(mockWSRequest1.withHeaders(any())).thenReturn(mockWSRequest2)

      when(mockWSClient.url(any())).thenReturn(mockWSRequest1)

      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))

      if(connector.getSubmissionBundle(testAuditId, testArchiveId) != null) {
        verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreDownload])(any())
      }
    }
  }

  override protected def beforeEach(): Unit = {
    reset(mockAuditConnector)
    reset(mockAuditable)
  }

  private val mockWsHttp = mock[WSHttpT]
  private val mockEnvironemnt = mock[Environment]
  private val mockHttpResponse = mock[HttpResponse]
  private val mockWSResponse = mock[WSResponse]
  private val mockAuditConnector = mock[AuditConnector]
  private val mockAuditable = mock[Auditable]
  private val mockWSClient = mock[WSClient]
  when(mockHttpResponse.body).thenReturn("Some Text")

  private val testModule = new AbstractModule {
    override def configure(): Unit = {
      bind(classOf[WSHttpT]).toInstance(mockWsHttp)
      bind(classOf[Environment]).toInstance(mockEnvironemnt)
      bind(classOf[AppConfig]).toInstance(mockAppConfig)
      bind(classOf[Auditable]).toInstance(mockAuditable)
      bind(classOf[AuditConnector]).toInstance(mockAuditConnector)
      bind(classOf[WSClient]).toInstance(mockWSClient)
      bind(classOf[Audit]).to(classOf[MicroserviceAudit])
      bind(classOf[String]).annotatedWith(Names.named("appName")).toProvider(AppNameProvider)
    }

    private object AppNameProvider extends Provider[String] {
      def get(): String = "nrs-retrieval"
    }
  }

  private val injector: Injector = Guice.createInjector(testModule)
  private val connector = injector.getInstance(classOf[NrsRetrievalConnector])

  private val testAuditId = "1"
  private val testArchiveId = "2"

}
