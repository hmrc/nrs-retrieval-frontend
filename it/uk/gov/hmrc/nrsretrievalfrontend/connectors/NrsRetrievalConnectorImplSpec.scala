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

import connectors.NrsRetrievalConnector
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import org.scalatest.concurrent.Eventually
import play.api.mvc.Call
import play.api.test.Helpers.baseApplicationBuilder.injector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nrsretrievalfrontend.BaseSpec
import uk.gov.hmrc.nrsretrievalfrontend.stubs.NrsRetrievalStubs.givenNrsSearchResult
import uk.gov.hmrc.nrsretrievalfrontend.wiremock.WireMockSupport

//add correct service stub name

class NrsRetrievalConnectorImplSpec extends BaseSpec with WireMockSupport with Eventually {
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val connector = injector.instanceOf[NrsRetrievalConnector]

  /*private lazy val connector: NrsRetrievalConnectorImpl =
    new NrsRetrievalConnectorImpl(
      app.injector.instanceOf[HttpClient],
      app.injector.instanceOf[Metrics],
      app.injector.instanceOf[AppConfig])
*/

  "search response" in {
    givenNrsSearchResult(server = wireMockServer)

    val response: String = connector.NrsRetrievalConnector(Call("GET", "/submission-metadata?")).futureValue

    response mustBe "/blah"
  }
}

