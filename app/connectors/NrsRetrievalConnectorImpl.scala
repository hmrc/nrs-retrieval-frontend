/*
 * Copyright 2020 HM Revenue & Customs
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
import config.{AppConfig, Auditable, WSHttpT}
import models.{AuthorisedUser, NrsSearchResult, SearchQuery}
import models.audit.{NonRepudiationStoreDownload, NonRepudiationStoreRetrieve, NonRepudiationStoreSearch}
import play.api.libs.ws.{WSClient, WSResponse}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

@Singleton
class NrsRetrievalConnectorImpl @Inject()(val environment: Environment,
                                          val http: WSHttpT,
                                          val auditable: Auditable,
                                          ws: WSClient,
                                          implicit val appConfig: AppConfig) extends NrsRetrievalConnector {

  val logger: Logger = Logger(this.getClass)

  override def search(query: SearchQuery, user: AuthorisedUser)(implicit hc: HeaderCarrier): Future[Seq[NrsSearchResult]] = {
    logger.info(s"Search for ${query.searchText}")

    val path = s"${appConfig.nrsRetrievalUrl}/submission-metadata?${query.searchText}"

    for{
      get <- http.GET[Seq[NrsSearchResult]](path)
        .map { r => r }
        .recover{
          case e if e.getMessage.contains("404") => Seq.empty[NrsSearchResult]
          case e if e.getMessage.contains("401") => {
            auditable.sendDataEvent(NonRepudiationStoreSearch(user.authProviderId, user.userName, query.searchText, "Unauthorized", path))
            throw e
          }
        }
      _ <- auditable.sendDataEvent(
        NonRepudiationStoreSearch(user.authProviderId, user.userName, query.searchText, get.seq.headOption.map(_.nrSubmissionId).getOrElse("(Empty)") ,path))
    } yield get
  }

  override def submitRetrievalRequest(vaultName: String, archiveId: String, user: AuthorisedUser)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    logger.info(s"Submit a retrieval request for vault: $vaultName, archive: $archiveId")

    val path = s"${appConfig.nrsRetrievalUrl}/submission-bundles/$vaultName/$archiveId/retrieval-requests"

    for {
      post <- http.POST[String, HttpResponse](path, "", Seq.empty)
      _ <- auditable.sendDataEvent(NonRepudiationStoreRetrieve(user.authProviderId, user.userName, vaultName, archiveId,
        if(post.allHeaders == null) "(Empty)" else post.header("nr-submission-id").getOrElse("(Empty)"), path))
    } yield post
  }

  override def statusSubmissionBundle(vaultName: String, archiveId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    logger.info(s"Get submission bundle status for vault: $vaultName, archive: $archiveId")
    val path = s"${appConfig.nrsRetrievalUrl}/submission-bundles/$vaultName/$archiveId"
    http.HEAD(path)
  }

  override def getSubmissionBundle(vaultName: String, archiveId: String, user: AuthorisedUser)(implicit hc: HeaderCarrier): Future[WSResponse] = {
    logger.info(s"Get submission bundle for vault: $vaultName, archive: $archiveId")
    val path = s"${appConfig.nrsRetrievalUrl}/submission-bundles/$vaultName/$archiveId"

    for{
      get <- ws.url(path).withHeaders(hc.headers ++ hc.extraHeaders ++ hc.otherHeaders: _*).get
      _ <- auditable.sendDataEvent(
        NonRepudiationStoreDownload(user.authProviderId, user.userName, vaultName, archiveId, get.header("nr-submission-id").getOrElse("(Empty)"), path))
    }yield get
  }

  protected def mode: Mode = environment.mode

}
