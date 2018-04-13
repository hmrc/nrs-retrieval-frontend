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

package actors

import uk.gov.hmrc.http.HeaderCarrier

trait ActorMessage

case class SubmitMessage(vaultId: String, archiveId: String, headerCarrier: HeaderCarrier) extends ActorMessage

case class StatusMessage(vaultId: String, archiveId: String) extends ActorMessage

case object IncompleteMessage extends ActorMessage

case object CompleteMessage extends ActorMessage

case class FailedMessage(payload: String) extends ActorMessage

case object UnknownMessage extends ActorMessage

case object PollingMessage extends ActorMessage

case object FailedToStartMessage extends ActorMessage

case object StartedMessage extends ActorMessage

case object RestartMessage extends ActorMessage
