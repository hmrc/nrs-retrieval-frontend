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

package uk.gov.hmrc.nrsretrievalfrontend.config

import com.google.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nrsretrievalfrontend.models.audit.DataEventAuditType
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class Auditable @Inject()(
                           @Named("appName") val applicationName: String,
                           val audit: Audit)(using executionContext: ExecutionContext):

  private val logger = Logger(this.getClass)

  // This only has side-effects, making a fire and forget call to an external system
  def sendDataEvent(dataEventAuditType: DataEventAuditType)(using hc: HeaderCarrier): Future[Unit] = {
    val event = DataEvent(
      dataEventAuditType.auditSource,
      dataEventAuditType.auditType,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(dataEventAuditType.transactionName, dataEventAuditType.path)
        ++ dataEventAuditType.tags.tags,
      detail = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails(dataEventAuditType.details.details.toSeq *)
    )
    logger.debug(s"Audit event: ${event.toString}")
    Future(audit.sendDataEvent(event))
  }