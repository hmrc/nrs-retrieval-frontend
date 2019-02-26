/*
 * Copyright 2019 HM Revenue & Customs
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
import com.google.inject.{AbstractModule, Guice, Injector}
import config.{AppConfig, Auditable, MicroserviceAudit, WSHttpT}
import javax.inject.Provider
import models.{AuthorisedUser, NrsSearchResult}
import models.audit.{DataEventAuditType, NonRepudiationStoreDownload, NonRepudiationStoreRetrieve, NonRepudiationStoreSearch}
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.Environment
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import support.fixtures.{Infrastructure, NrsSearchFixture}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class NrsRetrievalConnectorSpec extends UnitSpec with MockitoSugar with NrsSearchFixture with Infrastructure with BeforeAndAfterEach {

  "search" should {
    "make a get call to /submission-metadata returning data" in {
      when(mockWsHttp.GET[Seq[NrsSearchResult]](any())(any(), any(), any())).thenReturn(Future.successful(Seq(nrsVatSearchResult)))
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
      await(connector.search(searchQuery, testUser)).size shouldBe 1
      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])(any())
    }

    "make a get call to /submission-metadata with parameters returning no data" in {
      when(mockWsHttp.GET[Seq[NrsSearchResult]](any())(any(), any(), any())).thenReturn(Future.failed(new Throwable("404")))
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
      await(connector.search(searchQuery, testUser)).size shouldBe 0
      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])(any())
    }

    "make a get call to /submission-metadata with parameters resulting in a failure" in {
      when(mockWsHttp.GET[Seq[NrsSearchResult]](any())(any(), any(), any())).thenReturn(Future.failed(new Throwable("401")))
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
      a[Throwable] should be thrownBy await(connector.search(searchQuery, testUser))
      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])(any())
    }

    "make a get call to /submission-metadata and retrieve nr-submission-id from header" in {
      when(mockWsHttp.GET[Seq[NrsSearchResult]](any())(any(), any(), any())).thenReturn(Future.successful(Seq(nrsVatSearchResult)))
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenAnswer(new Answer[Future[Unit]](){
        override def answer(invocationOnMock: InvocationOnMock): Future[Unit] = {
          if(invocationOnMock.getArgumentAt(0, classOf[DataEventAuditType]).details.details("nrSubmissionId") == nrSubmissionId){
            Future.successful(())
          }else{
            Future.failed(new Throwable())
          }
        }
      })
      await(connector.search(searchQuery, testUser)).head.nrSubmissionId shouldBe nrSubmissionId
      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])(any())
    }

    "write an audit record containing the required data" in {
      when(mockWsHttp.GET[Seq[NrsSearchResult]](any())(any(), any(), any())).thenReturn(Future.successful(Seq(nrsVatSearchResult)))
      when(mockAuditable.sendDataEvent(any[NonRepudiationStoreSearch])(any())).thenReturn(Future.successful(()))
      await(connector.search(searchQuery, testUser)).size shouldBe 1
      val searchAudit = NonRepudiationStoreSearch("anAuthProviderId", "aUser", "notableEvent=aNotableEvent&aName=aValue", nrsVatSearchResult.nrSubmissionId, "null/submission-metadata?notableEvent=aNotableEvent&aName=aValue")
      verify(mockAuditable, times(1)).sendDataEvent(searchAudit)(hc)
    }

 }

  "submitRetrievalRequest" should {
    "make a post call to /retrieval-requests" in {
      when(mockWsHttp.POST[Any, Any](any(), any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(mockHttpResponse))
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
      await(connector.submitRetrievalRequest(testAuditId, testArchiveId, testUser)).body should be("Some Text")
      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreRetrieve])(any())
    }
    "make a post call to /retrieval-requests and retrieve nr-submission-id from header" in {
      when(mockWsHttp.POST[Any, Any](any(), any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(mockHttpResponse))
      when(mockHttpResponse.allHeaders).thenReturn(Map("nr-submission-id" -> Seq(nrSubmissionId)))
      when(mockHttpResponse.header("nr-submission-id")).thenReturn(Some(nrSubmissionId))
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenAnswer(new Answer[Future[Unit]](){
        override def answer(invocationOnMock: InvocationOnMock): Future[Unit] = {
          if(invocationOnMock.getArgumentAt(0, classOf[DataEventAuditType]).details.details("nrSubmissionId") == nrSubmissionId){
            Future.successful(())
          }else{
            Future.failed(new Throwable())
          }
        }
      })
      await(connector.submitRetrievalRequest(testAuditId, testArchiveId, testUser)).header("nr-submission-id") shouldBe Some(nrSubmissionId)
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
      when(mockWSRequest2.get).thenReturn(Future.successful(mockWSResponse))
      when(mockWSResponse.header(any())).thenReturn(Some("Some Header"))
      when(mockWSResponse.body).thenReturn("Some Text")
      val mockWSRequest1 = mock[WSRequest]
      when(mockWSRequest1.withHeaders(any())).thenReturn(mockWSRequest2)
      when(mockWSClient.url(any())).thenReturn(mockWSRequest1)
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
      await(connector.getSubmissionBundle(testAuditId, testArchiveId, testUser)).body shouldBe "Some Text"
      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreDownload])(any())
    }

    "make a get call to /submission-bundles and retrieve nr-submission-id from header" in {
      when(mockWsHttp.GET[Any](any())(any(), any(), any())).thenReturn(Future.successful(mockHttpResponse))
      val mockWSRequest2 = mock[WSRequest]
      when(mockWSRequest2.get).thenReturn(Future.successful(mockWSResponse))
      when(mockWSResponse.header(any())).thenReturn(Some("Some Header"))
      when(mockWSResponse.body).thenReturn("Some Text")
      val mockWSRequest1 = mock[WSRequest]
      when(mockWSRequest1.withHeaders(any())).thenReturn(mockWSRequest2)
      when(mockWSClient.url(any())).thenReturn(mockWSRequest1)
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenAnswer(new Answer[Future[Unit]](){
        override def answer(invocationOnMock: InvocationOnMock): Future[Unit] = {
          if(invocationOnMock.getArgumentAt(0, classOf[DataEventAuditType]).details.details("nrSubmissionId") == nrSubmissionId){
            Future.successful(())
          }else{
            Future.failed(new Throwable())
          }
        }
      })
      await(connector.submitRetrievalRequest(testAuditId, testArchiveId, testUser)).header("nr-submission-id") shouldBe Some(nrSubmissionId)
      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreRetrieve])(any())
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
      bind(classOf[NrsRetrievalConnector]).to(classOf[NrsRetrievalConnectorImpl])
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

  private val testUser: AuthorisedUser = AuthorisedUser("aUser", "anAuthProviderId")

}
