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

import play.api.Logger
import play.api.libs.ws.DefaultBodyWritables.writeableOf_String
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson
import uk.gov.hmrc.http.client.{HttpClientV2, readStreamHttpResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.nrsretrievalfrontend.config.{AppConfig, Auditable}
import uk.gov.hmrc.nrsretrievalfrontend.models.audit.{NonRepudiationStoreDownload, NonRepudiationStoreRetrieve, NonRepudiationStoreSearch}
import uk.gov.hmrc.nrsretrievalfrontend.models.{AuthorisedUser, NrsSearchResult, Query}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsRetrievalConnectorImpl @Inject() (
  val httpClient: HttpClientV2,
  val auditable: Auditable
)(using val appConfig: AppConfig, executionContext: ExecutionContext)
    extends NrsRetrievalConnector:
  private val logger: Logger = Logger(this.getClass)

  private[connectors] val extraHeaders: Seq[(String, String)] = Seq(
    ("X-API-Key", appConfig.xApiKey)
  )

  override def search(
    notableEvent: String,
    query: List[Query],
    crossKeySearch: Boolean
  )(using
    hc: HeaderCarrier,
    user: AuthorisedUser
  ): Future[Seq[NrsSearchResult]] =
    val path = s"${appConfig.nrsRetrievalUrl}/submission-metadata"

    val queryParams: Seq[(String, String)] =
      Query.queryParams(notableEvent, query, crossKeySearch)

    for
      get <- httpClient
               .get(url"$path")
               .transform(_.withQueryStringParameters(queryParams*))
               .setHeader(extraHeaders*)
               .execute[Seq[NrsSearchResult]]
               .map(r => r)
               .recover {
                 case e if e.getMessage.contains("404") => Seq.empty[NrsSearchResult]
                 case e if e.getMessage.contains("401") =>
                   auditable.sendDataEvent(
                     NonRepudiationStoreSearch(
                       user.authProviderId,
                       queryParams,
                       "Unauthorized",
                       path
                     )
                   )
                   throw e
               }
      _   <- auditable.sendDataEvent(
               NonRepudiationStoreSearch(
                 user.authProviderId,
                 queryParams,
                 get.headOption.map(_.nrSubmissionId).getOrElse("(Empty)"),
                 path
               )
             )
    yield get

  override def submitRetrievalRequest(
    vaultName: String,
    archiveId: String
  )(using hc: HeaderCarrier, user: AuthorisedUser): Future[HttpResponse] =
    logger.info(
      s"Submit a retrieval request for vault: $vaultName, archive: $archiveId"
    )

    val path =
      s"${appConfig.nrsRetrievalUrl}/submission-bundles/$vaultName/$archiveId/retrieval-requests"

    for
      post <- httpClient
                .post(url"$path")
                .setHeader(extraHeaders*)
                .withBody("")
                .execute[HttpResponse]
      _    <- auditable.sendDataEvent(
                NonRepudiationStoreRetrieve(
                  user.authProviderId,
                  vaultName,
                  archiveId,
                  if (post.headers == null) "(Empty)"
                  else post.header("nr-submission-id").getOrElse("(Empty)"),
                  path
                )
              )
    yield post

  override def statusSubmissionBundle(vaultName: String, archiveId: String)(using
    hc: HeaderCarrier
  ): Future[HttpResponse] =
    val path =
      s"${appConfig.nrsRetrievalUrl}/submission-bundles/$vaultName/$archiveId"

    logger.info(
      s"Get submission bundle status for vault: $vaultName, archive: $archiveId, path: $path"
    )

    httpClient
      .head(url"$path")
      .setHeader(extraHeaders*)
      .execute[HttpResponse]

  override def getSubmissionBundle(vaultName: String, archiveId: String)(using
    hc: HeaderCarrier,
    user: AuthorisedUser
  ): Future[HttpResponse] =
    val path =
      s"${appConfig.nrsRetrievalUrl}/submission-bundles/$vaultName/$archiveId"

    logger.info(
      s"Get submission bundle for vault: $vaultName, archive: $archiveId, path: $path"
    )

    httpClient
      .get(url"$path")
      .setHeader(extraHeaders*)
      .stream[
        HttpResponse
      ] // .stream[HttpResponse] required as execute causes issues while deploying the .zip bundle (unextractable due to charset parsing)
      .map { get =>
        auditable.sendDataEvent(
          NonRepudiationStoreDownload(
            authProviderId = user.authProviderId,
            vaultName = vaultName,
            archiveId = archiveId,
            nrSubmissionId = get.header("nr-submission-id").getOrElse("(Empty)"),
            path = path
          )
        )

        get
      }
