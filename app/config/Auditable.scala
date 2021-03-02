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

package config

import com.google.inject.Inject
import javax.inject.Named
import models.audit.DataEventAuditType
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}
import scala.concurrent.ExecutionContext.Implicits.global


import scala.concurrent.Future

class Auditable @Inject()(@Named("appName") val applicationName: String, val audit: Audit){

  private val logger = Logger(this.getClass)

  // This only has side-effects, making a fire and forget call to an external system
  def sendDataEvent(dataEventAuditType: DataEventAuditType)(implicit hc: HeaderCarrier): Future[Unit] = {
    val event = DataEvent(
      dataEventAuditType.auditSource,
      dataEventAuditType.auditType,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(dataEventAuditType.transactionName, dataEventAuditType.path)
        ++ dataEventAuditType.tags.tags,
      detail = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails(dataEventAuditType.details.details.toSeq: _*)
    )
    logger.debug(s"Audit event: ${event.toString}")
    Future(audit.sendDataEvent(event))
  }

}
