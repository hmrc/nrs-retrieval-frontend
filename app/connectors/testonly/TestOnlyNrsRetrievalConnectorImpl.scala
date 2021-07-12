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

package connectors.testonly

import connectors.NrsRetrievalConnector
import models.AuthorisedUser
import models.testonly.ValidateDownloadResult
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TestOnlyNrsRetrievalConnectorImpl @Inject()(nrsRetrievalConnector: NrsRetrievalConnector) extends TestOnlyNrsRetrievalConnector{
  override def validateDownload(vaultName: String, archiveId: String, user: AuthorisedUser)
                               (implicit hc: HeaderCarrier): Future[ValidateDownloadResult] =
    nrsRetrievalConnector.getSubmissionBundle(vaultName, archiveId, user).map(response => ValidateDownloadResult(response))
}
