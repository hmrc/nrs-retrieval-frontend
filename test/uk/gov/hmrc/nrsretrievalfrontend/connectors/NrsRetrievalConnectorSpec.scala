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

package uk.gov.hmrc.nrsretrievalfrontend.connectors

import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Guice, Injector}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.BeforeAndAfterEach
import play.api.Environment
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.nrsretrievalfrontend.config.{AppConfig, Auditable}
import uk.gov.hmrc.nrsretrievalfrontend.http.{MicroserviceAudit, WSHttpT}
import uk.gov.hmrc.nrsretrievalfrontend.models.audit.{DataEventAuditType, NonRepudiationStoreDownload, NonRepudiationStoreRetrieve, NonRepudiationStoreSearch}
import uk.gov.hmrc.nrsretrievalfrontend.models.{AuthorisedUser, NrsSearchResult, Query}
import uk.gov.hmrc.nrsretrievalfrontend.support.UnitSpec
import uk.gov.hmrc.nrsretrievalfrontend.support.fixtures.NrsSearchFixture
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit

import javax.inject.Provider
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class NrsRetrievalConnectorSpec extends UnitSpec with NrsSearchFixture with BeforeAndAfterEach {
  override protected def beforeEach(): Unit = {
    reset(mockAuditConnector)
    reset(mockAuditable)
  }

  private val mockWsHttp = mock[WSHttpT]
  private val mockEnvironment = mock[Environment]
  private val mockHttpResponse = mock[HttpResponse]
  private val mockWSResponse = mock[WSResponse]
  private val mockAuditConnector = mock[AuditConnector]
  private val mockAuditable = mock[Auditable]
  private val mockWSClient = mock[WSClient]
  private val mockAppConfig = mock[AppConfig]

  when(mockHttpResponse.body).thenReturn("Some Text")

  private val testModule = new AbstractModule {
    override def configure(): Unit = {
      bind(classOf[ExecutionContext]).toInstance(global)
      bind(classOf[NrsRetrievalConnector]).to(classOf[NrsRetrievalConnectorImpl])
      bind(classOf[WSHttpT]).toInstance(mockWsHttp)
      bind(classOf[Environment]).toInstance(mockEnvironment)
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
  private val connector = injector.getInstance(classOf[NrsRetrievalConnectorImpl])
  private val testAuditId = "1"
  private val testArchiveId = "2"
  private implicit val testUser: AuthorisedUser = AuthorisedUser("aUser", "anAuthProviderId")

  private def expectedExtraHeaders = Matchers.eq(connector.extraHeaders)

  "extraHeaders" should {
    "contain the X-API-Key header" in {
      connector.extraHeaders.contains(("X-API-Key", injector.getInstance(classOf[AppConfig]).xApiKey)) shouldBe true
    }
  }

  "search" should {
    Seq(false, true).foreach { crossKeySearch =>
      val url = s"null/submission-metadata"
      val queryParams = Query.queryParams(notableEvent, searchParams, crossKeySearch)
      val searchType = if (crossKeySearch) "cross key" else "standard"

      "make a get call to /submission-metadata returning data" when {
        s"a $searchType search is requested" in {
          when(mockWsHttp.GET[Seq[NrsSearchResult]](Matchers.eq(url), Matchers.eq(queryParams), expectedExtraHeaders)(any(), any(), any()))
            .thenReturn(Future.successful(Seq(nrsVatSearchResult)))
          when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
          await(connector.search(notableEvent, searchParams, crossKeySearch)).size shouldBe 1
          verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])(any())
        }
      }

      "make a get call to /submission-metadata with parameters returning no data" when {
        s"a $searchType search is requested" in {
          when(mockWsHttp.GET[Seq[NrsSearchResult]](Matchers.eq(url), Matchers.eq(queryParams), expectedExtraHeaders)(any(), any(), any()))
            .thenReturn(Future.failed(new Throwable("404")))
          when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
          await(connector.search(notableEvent, searchQuery.queries, crossKeySearch)).size shouldBe 0
          verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])(any())
        }
      }

      "make a get call to /submission-metadata with parameters resulting in a failure" when {
        s"a $searchType search is requested" in {
          when(mockWsHttp.GET[Seq[NrsSearchResult]](Matchers.eq(url), Matchers.eq(queryParams), expectedExtraHeaders)(any(), any(), any()))
            .thenReturn(Future.failed(new Throwable("401")))
          when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
          a[Throwable] should be thrownBy await(connector.search(notableEvent, searchQuery.queries, crossKeySearch))
          verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])(any())
        }
      }

      "make a get call to /submission-metadata and retrieve nr-submission-id from header" when {
        s"a $searchType search is requested" in {
          when(mockWsHttp.GET[Seq[NrsSearchResult]](Matchers.eq(url), Matchers.eq(queryParams), expectedExtraHeaders)(any(), any(), any()))
            .thenReturn(Future.successful(Seq(nrsVatSearchResult)))
          when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenAnswer(new Answer[Future[Unit]](){
            override def answer(invocationOnMock: InvocationOnMock): Future[Unit] = {
              if(invocationOnMock.getArgumentAt(0, classOf[DataEventAuditType]).details.details("nrSubmissionId") == nrSubmissionId){
                Future.successful(())
              }else{
                Future.failed(new Throwable())
              }
            }
          })
          await(connector.search(notableEvent, searchQuery.queries, crossKeySearch)).head.nrSubmissionId shouldBe nrSubmissionId
          verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])(any())
        }
      }

      "write an audit record containing the required data" when {
        s"a $searchType search is requested" in {
          when(mockWsHttp.GET[Seq[NrsSearchResult]](Matchers.eq(url), any(), expectedExtraHeaders)(any(), any(), any()))
            .thenReturn(Future.successful(Seq(nrsVatSearchResult)))
          when(mockAuditable.sendDataEvent(any[NonRepudiationStoreSearch])(any())).thenReturn(Future.successful(()))
          await(connector.search(notableEvent, searchParams, crossKeySearch)).size shouldBe 1

          verify(mockAuditable, times(1)).sendDataEvent(NonRepudiationStoreSearch(
            "anAuthProviderId", "aUser", queryParams, nrsVatSearchResult.nrSubmissionId, url))(hc)
        }
      }
    }
 }

  "submitRetrievalRequest" should {
    "make a post call to /retrieval-requests" in {
      when(mockWsHttp.POSTEmpty[Any](any(), expectedExtraHeaders)(any(), any(), any())).thenReturn(Future.successful(mockHttpResponse))
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
      await(connector.submitRetrievalRequest(testAuditId, testArchiveId)).body should be("Some Text")
      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreRetrieve])(any())
    }

    "make a post call to /retrieval-requests and retrieve nr-submission-id from header" in {
      when(mockWsHttp.POST[Any, Any](any(), any(), expectedExtraHeaders)(any(), any(), any(), any())).thenReturn(Future.successful(mockHttpResponse))
      when(mockHttpResponse.headers).thenReturn(Map("nr-submission-id" -> Seq(nrSubmissionId)))
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
      await(connector.submitRetrievalRequest(testAuditId, testArchiveId)).header("nr-submission-id") shouldBe Some(nrSubmissionId)
      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreRetrieve])(any())
    }
  }

  "statusSubmissionBundle" should {
    "make a head call to /submission-bundles" in {
      when(mockWsHttp.HEAD(any(), expectedExtraHeaders)(any(), any())).thenReturn(Future.successful(mockHttpResponse))
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
      await(connector.statusSubmissionBundle(testAuditId, testArchiveId)).body should be ("Some Text")
      verify(mockAuditable, times(0)).sendDataEvent(any[NonRepudiationStoreDownload])(any())
    }
  }

  "getSubmissionBundle" should {
    "make a get call to /submission-bundles" in {
      when(mockWsHttp.GETRaw(any(), expectedExtraHeaders)(any(), any())).thenReturn(Future.successful(mockWSResponse))
      when(mockWSResponse.header(any())).thenReturn(Some("Some Header"))
      when(mockWSResponse.body).thenReturn("Some zipped bytes")
      when(mockAuditable.sendDataEvent(any[DataEventAuditType])(any())).thenReturn(Future.successful(()))
      await(connector.getSubmissionBundle(testAuditId, testArchiveId)).body shouldBe "Some zipped bytes"
      verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreDownload])(any())
    }
  }
}
