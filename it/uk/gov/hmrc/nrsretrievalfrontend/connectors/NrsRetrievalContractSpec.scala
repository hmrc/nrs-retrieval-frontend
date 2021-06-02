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

package uk.gov.hmrc.nrsretrievalfrontend.connectors

import com.github.tomakehurst.wiremock.client.WireMock
import connectors.NrsRetrievalConnector
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.baseApplicationBuilder.injector
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.nrsretrievalfrontend.stubs.NrsRetrievalStubs._
import uk.gov.hmrc.nrsretrievalfrontend.wiremock.WireMockSupport

class NrsRetrievalContractSpec
  extends WordSpecLike
    with Matchers
    with ScalaFutures
    with GuiceOneServerPerSuite
    with WireMockSupport
    with IntegrationPatience
    with BeforeAndAfterEach {
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val connector = injector.instanceOf[NrsRetrievalConnector]

  override def beforeEach(): Unit = WireMock.reset()

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(Map[String, Any](
      "microservice.services.nrs-retrieval.port" -> wireMockPort, //not effective - had to set wiremock port to configured nrs port. Wy???
      "auditing.enabled" -> false,
      "metrics.jvm" -> false)
    ).build()

  "statusSubmissionBundle" should {
    def statusSubmissionBundle(): HttpResponse =
      connector.statusSubmissionBundle("vaultName", "archiveId").futureValue

    "return OK" when {
      "the retrieval service returns OK" in {
        givenHeadSubmissionBundlesReturns(OK)
        statusSubmissionBundle().status shouldBe OK
      }
    }

    "return ACCEPTED" when {
      "the retrieval service returns ACCEPTED" in {
        givenHeadSubmissionBundlesReturns(ACCEPTED)
        statusSubmissionBundle().status shouldBe ACCEPTED
      }
    }

    "fail" when {
      "the retrieval service returns NOT_FOUND" in {
        givenHeadSubmissionBundlesReturns(NOT_FOUND)

        intercept[Exception] {
          statusSubmissionBundle()
        }.getCause.asInstanceOf[HttpException].responseCode shouldBe NOT_FOUND
      }

      "the retrieval service returns INTERNAL_SERVER_ERROR" in {
        givenHeadSubmissionBundlesReturns(INTERNAL_SERVER_ERROR)

        intercept[Exception] {
          statusSubmissionBundle()
        }.getCause.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe INTERNAL_SERVER_ERROR
      }

      "the retrieval service returns BAD_GATEWAY" in {
        givenHeadSubmissionBundlesReturns(BAD_GATEWAY)

        intercept[Exception] {
          statusSubmissionBundle()
        }.getCause.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe BAD_GATEWAY
      }
    }
  }
}

