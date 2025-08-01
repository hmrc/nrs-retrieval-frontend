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

import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import uk.gov.hmrc.nrsretrievalfrontend.models.audit.NonRepudiationStoreSearch
import uk.gov.hmrc.nrsretrievalfrontend.support.UnitSpec
import uk.gov.hmrc.play.audit.model.*

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

abstract class AuditableSpec extends UnitSpec:
  private val appName                      = "TestAppName"
  private val mockAudit: Audit             = mock[Audit]
  private val auditable                    = new Auditable(appName, mockAudit)
  private val ec: ExecutionContextExecutor = ExecutionContext.global
  val dataEvent: DataEvent

  "sendDataEvent" should {
    "send a NonRepudiationStoreSearch audit event" in {
      val dataEventAuditType = NonRepudiationStoreSearch("authProviderId", Seq("vatReturnVRN" -> "validVrn"), "nrSubmissionId", "path")

      val func: DataEvent => Unit = mock[DataEvent => Unit]
      when(func.apply(any())).thenReturn(())
      when(mockAudit.sendDataEvent(dataEvent)(ec)).thenReturn(())

      await(auditable.sendDataEvent(dataEventAuditType))
      verify(mockAudit, times(1)).sendDataEvent(dataEvent)
    }
  }
