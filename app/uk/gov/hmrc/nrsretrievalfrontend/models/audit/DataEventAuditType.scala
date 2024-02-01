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

package uk.gov.hmrc.nrsretrievalfrontend.models.audit

import java.net.URLEncoder

trait DataEventAuditType {
  val auditSource = "nrs-retrieval"
  val submissionType = "vat-return"
  val auditType: String
  val transactionName: String
  val path: String

  def details: DataEventDetails

  def tags: DataEventTags = {
    DataEventTags(Map(
      "transactionName" -> transactionName
    ))
  }
}

case class NonRepudiationStoreSearch(authProviderId: String,
                                     name: String,
                                     searchParams: Seq[(String, String)],
                                     nrSubmissionId: String,
                                     override val path: String) extends DataEventAuditType {

  override val auditType: String = "nonRepudiationStoreSearch"
  override val transactionName = "Non-Repudiation Store search"

  val searchText = searchParams.map { case (k, v) => s"$k=${URLEncoder.encode(v, "utf-8")}" }.mkString("", "&", "")

  override def details: DataEventDetails = {
    DataEventDetails(Map(
      "authProviderId" -> authProviderId,
      "name" -> name,
      "submissionType" -> submissionType,
      "nrSubmissionId" -> nrSubmissionId,
      "searchText" -> searchText
    ))
  }
}

case class NonRepudiationStoreRetrieve(authProviderId: String,
                                       name: String,
                                       vaultName: String,
                                       archiveId: String,
                                       nrSubmissionId: String,
                                       override val path: String) extends DataEventAuditType {

  override val auditType: String = "nonRepudiationStoreRetrieve"
  override val transactionName = "Non-Repudiation Store retrieval"

  override def details: DataEventDetails = {
    DataEventDetails(Map(
      "authProviderId" -> authProviderId,
      "name" -> name,
      "submissionType" -> submissionType,
      "nrSubmissionId" -> nrSubmissionId,
      "vaultName" -> vaultName,
      "archiveId" -> archiveId
    ))
  }
}

case class NonRepudiationStoreDownload(authProviderId: String,
                                       name: String,
                                       vaultName: String,
                                       archiveId: String,
                                       nrSubmissionId: String,
                                       override val path: String) extends DataEventAuditType {

  override val auditType: String = "nonRepudiationStoreDownload"
  override val transactionName = "Non-Repudiation Store download"

  override def details: DataEventDetails = {
    DataEventDetails(Map(
      "authProviderId" -> authProviderId,
      "name" -> name,
      "submissionType" -> submissionType,
      "nrSubmissionId" -> nrSubmissionId,
      "vaultName" -> vaultName,
      "archiveId" -> archiveId
    ))
  }
}

case class DataEventDetails(details: Map[String, String])

case class DataEventTags(tags: Map[String, String])
