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

package models.audit

trait DataEventAuditType {
  val auditSource = "nrs-retrieval"
  val submissionType = "vat-return"
  val submissionId = "beb6f5b8-f2c3-4d5a-8c72-0888fc1bbbfd"
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
                                     VRN: String,
                                     override val path: String) extends DataEventAuditType {

  override val auditType: String = "nonRepudiationStoreSearch"
  override val transactionName = "Non-Repudiation Store search"

  override def details: DataEventDetails = {
    DataEventDetails(Map(
      "authProviderId" -> authProviderId,
      "name" -> name,
      "submissionType" -> submissionType,
      "nrSubmissionId" -> submissionId,
      "VRN" -> VRN
    ))
  }
}


case class NonRepudiationStoreRetrieve(authProviderId: String,
                                       name: String,
                                       vaultName: String,
                                       archiveId: String,
                                       override val path: String) extends DataEventAuditType {

  override val auditType: String = "nonRepudiationStoreRetrieve"
  override val transactionName = "Non-Repudiation Store retrieval"

  override def details: DataEventDetails = {
    DataEventDetails(Map(
      "authProviderId" -> authProviderId,
      "name" -> name,
      "submissionType" -> submissionType,
      "nrSubmissionId" -> submissionId,
      "vaultName" -> vaultName,
      "archiveId" -> archiveId
    ))
  }
}

case class NonRepudiationStoreDownload(authProviderId: String,
                                       name: String,
                                       vaultName: String,
                                       archiveId: String,
                                       override val path: String) extends DataEventAuditType {

  override val auditType: String = "nonRepudiationStoreDownload"
  override val transactionName = "Non-Repudiation Store download"

  override def details: DataEventDetails = {
    DataEventDetails(Map(
      "authProviderId" -> authProviderId,
      "name" -> name,
      "submissionType" -> submissionType,
      "nrSubmissionId" -> submissionId,
      "vaultName" -> vaultName,
      "archiveId" -> archiveId
    ))
  }
}

case class DataEventDetails(details: Map[String, String])

case class DataEventTags(tags: Map[String, String])
