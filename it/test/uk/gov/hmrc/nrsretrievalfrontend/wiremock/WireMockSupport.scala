/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.nrsretrievalfrontend.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.*
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Suite}
import play.api.http.Status

trait WireMockSupport extends BeforeAndAfterAll, BeforeAndAfter, Status:
  self: Suite =>
  val wireMockHost    = "localhost"
  val wireMockPort    = 19391
  val wireMockBaseUrl = s"http://$wireMockHost:$wireMockPort"

  lazy val wireMockServer = new WireMockServer(wireMockConfig().port(wireMockPort))

  configureFor(wireMockHost, wireMockPort)

  override protected def beforeAll(): Unit =
    super.beforeAll()
    wireMockServer.start()

  override protected def afterAll(): Unit =
    super.afterAll()
    wireMockServer.stop()
