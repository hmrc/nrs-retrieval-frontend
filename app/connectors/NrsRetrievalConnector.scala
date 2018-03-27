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

import javax.inject.{Inject, Singleton}

import play.api.{Environment, Logger}
import play.api.Mode.Mode
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import config.{AppConfig, WSHttpT}
import models.NrsSearchResult
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class NrsRetrievalConnector @Inject()(val environment: Environment,
                                      val http: WSHttpT,
                                      implicit val appConfig: AppConfig) {

  val logger: Logger = Logger(this.getClass)

  protected def mode: Mode = environment.mode

  def search(vrn: String)(implicit hc: HeaderCarrier): Future[Seq[NrsSearchResult]] = {
    logger.info(s"Search for VRN $vrn")
    http.GET[Seq[NrsSearchResult]](s"${appConfig.nrsRetrievalUrl}/submission-metadata?vrn=$vrn")
  }

  def submitRetrievalRequest(vaultId: Long, archiveId: Long)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    logger.info(s"Submit a retrieval request for vault: $vaultId, archive: $archiveId")
    http.doPostString(s"${appConfig.nrsRetrievalUrl}/submission-bundles/$vaultId/$archiveId/retrieval-requests", "", Seq.empty)
  }

  def statusSubmissionBundle(vaultId: Long, archiveId: Long)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    logger.info(s"Get submission bundle status for vault: $vaultId, archive: $archiveId")
    http.doHead(s"${appConfig.nrsRetrievalUrl}/submission-bundles/$vaultId/$archiveId")
  }

  def getSubmissionBundle(vaultId: Long, archiveId: Long)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    logger.info(s"Get submission bundle for vault: $vaultId, archive: $archiveId")
    http.doGet(s"${appConfig.nrsRetrievalUrl}/submission-bundles/$vaultId/$archiveId")
  }

}
