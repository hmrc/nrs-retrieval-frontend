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

package support.fixtures

import config.AppConfig
import models.{SearchResult, SearchResultUtils}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait SearchFixture extends NrSubmissionId {

  private val env = Environment.simple()
  private val configuration = Configuration.load(env)
  private val appConfig = new AppConfig(configuration, env, new ServicesConfig(configuration))

  val searchResultUtils: SearchResultUtils = new SearchResultUtils(appConfig)

  private val fileName = s"$nrSubmissionId.zip"

  val vatSearchResult: SearchResult =
    SearchResult("VAT return", s"$fileName (120 KB)", fileName, "12345", "1234567890", 1511773625L, None)
}

