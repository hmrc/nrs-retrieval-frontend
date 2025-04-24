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
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.BeforeAndAfterEach
import play.api.Environment
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.nrsretrievalfrontend.config.{AppConfig, Auditable, MicroserviceAudit}
import uk.gov.hmrc.nrsretrievalfrontend.models.audit.{DataEventAuditType, NonRepudiationStoreDownload, NonRepudiationStoreRetrieve, NonRepudiationStoreSearch}
import uk.gov.hmrc.nrsretrievalfrontend.models.{AuthorisedUser, NrsSearchResult, Query}
import uk.gov.hmrc.nrsretrievalfrontend.support.UnitSpec
import uk.gov.hmrc.nrsretrievalfrontend.support.fixtures.NrsSearchFixture
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit

import java.net.URL
import javax.inject.Provider
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class NrsRetrievalConnectorSpec extends UnitSpec with NrsSearchFixture with BeforeAndAfterEach {
  override protected def beforeEach(): Unit = {
    reset(mockAuditConnector)
    reset(mockAuditable)
  }

  private val mockHttpClientV2 = mock[HttpClientV2]
  private val mockEnvironment = mock[Environment]
  private val mockHttpResponse = mock[HttpResponse]
  private val mockAuditConnector = mock[AuditConnector]
  private val mockAuditable = mock[Auditable]
  private val mockAppConfig = mock[AppConfig]
  private val mockRequestBuilder = mock[RequestBuilder]


  private val testModule = new AbstractModule {
    override def configure(): Unit = {
      bind(classOf[ExecutionContext]).toInstance(global)
      bind(classOf[NrsRetrievalConnector]).to(classOf[NrsRetrievalConnectorImpl])
      bind(classOf[HttpClientV2]).toInstance(mockHttpClientV2)
      bind(classOf[Environment]).toInstance(mockEnvironment)
      bind(classOf[AppConfig]).toInstance(mockAppConfig)
      bind(classOf[Auditable]).toInstance(mockAuditable)
      bind(classOf[AuditConnector]).toInstance(mockAuditConnector)
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
  given testUser: AuthorisedUser = AuthorisedUser("anAuthProviderId")
  private val submissionBundlesUrl = new URL("http://localhost:19391/nrs-retrieval/submission-bundles/vat-return/vrn/retrieval-requests")
  private val submissionMetadataUrl = s"null/submission-metadata"

  "extraHeaders" should {
    "contain the X-API-Key header" in {
      connector.extraHeaders.contains(("X-API-Key", injector.getInstance(classOf[AppConfig]).xApiKey)) shouldBe true
    }
  }

  "search" should {
    Seq(false, true).foreach { crossKeySearch =>
      val queryParams = Query.queryParams(notableEvent, searchParams, crossKeySearch)
      val searchType = if (crossKeySearch) "cross key" else "standard"

      "make a get call to /submission-metadata returning data" when {
        s"a $searchType search is requested" in {
          (dataEventAuditType: DataEventAuditType) =>
            when(mockHttpClientV2.get(any())(using any[HeaderCarrier])).thenReturn(Future.successful(Seq(nrsVatSearchResult)))
            when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
            when(mockRequestBuilder.execute[Seq[NrsSearchResult]](any(), any())).thenReturn(Future.successful[Seq[NrsSearchResult]])

            when(mockAuditable.sendDataEvent(ArgumentMatchers.eq(dataEventAuditType))).thenReturn(Future.successful(()))
            await(connector.search(notableEvent, searchParams, crossKeySearch)).size shouldBe 1
            verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])
        }
      }

      "make a get call to /submission-metadata with parameters returning no data" when {
        s"a $searchType search is requested" in {
          (dataEventAuditType: DataEventAuditType) =>
            when(mockHttpClientV2.get(any())(using any[HeaderCarrier])).thenReturn(Future.failed(new Throwable("404")))
            when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
            when(mockRequestBuilder.execute[Seq[NrsSearchResult]](any(), any())).thenReturn(Future.successful[Seq[NrsSearchResult]])

            when(mockAuditable.sendDataEvent(ArgumentMatchers.eq(dataEventAuditType))).thenReturn(Future.successful(()))
            await(connector.search(notableEvent, searchQuery.queries, crossKeySearch)).size shouldBe 0
            verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])


        }
      }

      "make a get call to /submission-metadata with parameters resulting in a failure" when {
        s"a $searchType search is requested" in {
          (dataEventAuditType: DataEventAuditType) =>
            when(mockHttpClientV2.get(any())(using any[HeaderCarrier])).thenReturn(Future.failed(new Throwable("401")))
            when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
            when(mockRequestBuilder.execute[Seq[NrsSearchResult]](any(), any())).thenReturn(Future.successful[Seq[NrsSearchResult]])


            when(mockAuditable.sendDataEvent(ArgumentMatchers.eq(dataEventAuditType))).thenReturn(Future.successful(()))
            a[Throwable] should be thrownBy await(connector.search(notableEvent, searchQuery.queries, crossKeySearch))
            verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])
        }
      }

      "make a get call to /submission-metadata and retrieve nr-submission-id from header" when {
        s"a $searchType search is requested" in {
          (dataEventAuditType: DataEventAuditType) =>
            when(mockHttpClientV2.get(any())(using any[HeaderCarrier])).thenReturn(Future.successful(Seq(nrsVatSearchResult)))
            when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
            when(mockRequestBuilder.execute[Seq[NrsSearchResult]](any(), any())).thenReturn(Future.successful[Seq[NrsSearchResult]])

            when(mockAuditable.sendDataEvent(ArgumentMatchers.eq(dataEventAuditType))).thenAnswer(new Answer[Future[Unit]]() {
              override def answer(invocationOnMock: InvocationOnMock): Future[Unit] = {
                if (invocationOnMock.getArgument(0, classOf[DataEventAuditType]).details.details("nrSubmissionId") == nrSubmissionId) {
                  Future.successful(())
                } else {
                  Future.failed(new Throwable())
                }
              }
            })
            await(connector.search(notableEvent, searchQuery.queries, crossKeySearch)).head.nrSubmissionId shouldBe nrSubmissionId
            verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreSearch])
        }
      }

      "write an audit record containing the required data" when {
        s"a $searchType search is requested" in {
          (dataEventAuditType: DataEventAuditType) =>
            when(mockHttpClientV2.get(any())(using any[HeaderCarrier])).thenReturn(Future.successful(Seq(nrsVatSearchResult)))
            when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
            when(mockRequestBuilder.execute[Seq[NrsSearchResult]](any(), any())).thenReturn(Future.successful[Seq[NrsSearchResult]])
            when(mockAuditable.sendDataEvent(any[NonRepudiationStoreSearch])).thenReturn(Future.successful(()))
            await(connector.search(notableEvent, searchParams, crossKeySearch)).size shouldBe 1
            verify(mockAuditable, times(1)).sendDataEvent(NonRepudiationStoreSearch(
              "anAuthProviderId", queryParams, nrsVatSearchResult.nrSubmissionId, submissionMetadataUrl))(using hc)
        }
      }
    }
  }

  "submitRetrievalRequest" should {
    "make a post call to /retrieval-requests" in {
      (dataEventAuditType: DataEventAuditType) =>
        when(mockHttpClientV2.post(ArgumentMatchers.eq(submissionBundlesUrl))(using any[HeaderCarrier])).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(ArgumentMatchers.eq(""))(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(HttpResponse))
        when(mockAuditable.sendDataEvent(ArgumentMatchers.eq(dataEventAuditType))).thenReturn(Future.successful(()))
        await(connector.submitRetrievalRequest(testAuditId, testArchiveId)).body should be("Some Text")
        verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreRetrieve])
    }

    "make a post call to /retrieval-requests and retrieve nr-submission-id from header" in {
      (dataEventAuditType: DataEventAuditType) =>
        when(mockHttpClientV2.post(ArgumentMatchers.eq(submissionBundlesUrl))(using any[HeaderCarrier])).thenReturn(mockRequestBuilder)
        when(mockHttpResponse.headers).thenReturn(Map("nr-submission-id" -> Seq(nrSubmissionId)))
        when(mockHttpResponse.header("nr-submission-id")).thenReturn(Some(nrSubmissionId))
        when(mockRequestBuilder.withBody(ArgumentMatchers.eq(""))(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(HttpResponse))

        when(mockAuditable.sendDataEvent(ArgumentMatchers.eq(dataEventAuditType))).thenAnswer(new Answer[Future[Unit]]() {
          override def answer(invocationOnMock: InvocationOnMock): Future[Unit] = {
            if (invocationOnMock.getArgument(0, classOf[DataEventAuditType]).details.details("nrSubmissionId") == nrSubmissionId) {
              Future.successful(())
            } else {
              Future.failed(new Throwable())
            }
          }
        })
        await(connector.submitRetrievalRequest(testAuditId, testArchiveId)).header("nr-submission-id") shouldBe Some(nrSubmissionId)
        verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreRetrieve])
    }
  }

  "statusSubmissionBundle" should {
    "make a head call to /submission-bundles" in {
      (dataEventAuditType: DataEventAuditType) =>
        when(mockHttpClientV2.head(ArgumentMatchers.eq(submissionBundlesUrl))(using any[HeaderCarrier])).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockAuditable.sendDataEvent(ArgumentMatchers.eq(dataEventAuditType))).thenReturn(Future.successful(()))
        await(connector.statusSubmissionBundle(testAuditId, testArchiveId)).body should be("Some Text")
        verify(mockAuditable, times(0)).sendDataEvent(any[NonRepudiationStoreDownload])
    }
  }

  "getSubmissionBundle" should {
    "make a get call to /submission-bundles" in {
      (dataEventAuditType: DataEventAuditType) =>
        when(mockHttpClientV2.get(ArgumentMatchers.eq(submissionBundlesUrl))(using any[HeaderCarrier])).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockHttpResponse.bodyAsSource).thenReturn(Source.single(ByteString("Some zipped bytes")))
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(HttpResponse))

        when(mockAuditable.sendDataEvent(ArgumentMatchers.eq(dataEventAuditType))).thenReturn(Future.successful(()))
        await(connector.getSubmissionBundle(testAuditId, testArchiveId)).bodyAsSource should be("Some zipped bytes")
        verify(mockAuditable, times(1)).sendDataEvent(any[NonRepudiationStoreDownload])
    }
  }
}

