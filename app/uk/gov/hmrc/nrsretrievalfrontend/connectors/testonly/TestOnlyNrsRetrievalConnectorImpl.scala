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

package uk.gov.hmrc.nrsretrievalfrontend.connectors.testonly

import org.apache.pekko.actor.ActorSystem
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson
import uk.gov.hmrc.http.client.*
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.nrsretrievalfrontend.config.AppConfig
import uk.gov.hmrc.nrsretrievalfrontend.connectors.NrsRetrievalConnector
import uk.gov.hmrc.nrsretrievalfrontend.models.AuthorisedUser
import uk.gov.hmrc.nrsretrievalfrontend.models.testonly.ValidateDownloadResult

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestOnlyNrsRetrievalConnectorImpl @Inject() (
  nrsRetrievalConnector: NrsRetrievalConnector,
  http: HttpClientV2
)(using appConfig: AppConfig, executionContext: ExecutionContext, actorSystem: ActorSystem)
    extends TestOnlyNrsRetrievalConnector:

  override def validateDownload(vaultName: String, archiveId: String)(using
    hc: HeaderCarrier,
    user: AuthorisedUser
  ): Future[ValidateDownloadResult] =
    nrsRetrievalConnector
      .getSubmissionBundle(vaultName, archiveId)
      .flatMap(response => ValidateDownloadResult(response))

  override def checkAuthorisation(using hc: HeaderCarrier): Future[Boolean] =
    http
      .get(url"${appConfig.nrsRetrievalUrl}/test-only/check-authorisation")
      .execute[Boolean]
