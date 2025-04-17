/*
 * Copyright 2024 HM Revenue & Customs
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

import com.google.inject.{AbstractModule, Provides}
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.PekkoGuiceSupport
import play.api.{Configuration, Environment}
import uk.gov.hmrc.nrsretrievalfrontend.actions.NotableEventRefiner
import uk.gov.hmrc.nrsretrievalfrontend.connectors.testonly.{TestOnlyNrsRetrievalConnector, TestOnlyNrsRetrievalConnectorImpl}
import uk.gov.hmrc.nrsretrievalfrontend.connectors.{NrsRetrievalConnector, NrsRetrievalConnectorImpl}
import uk.gov.hmrc.nrsretrievalfrontend.http.MicroserviceAudit
import uk.gov.hmrc.nrsretrievalfrontend.views.html.error_template
import uk.gov.hmrc.play.audit.model.Audit

import javax.inject.Singleton
import scala.concurrent.ExecutionContext

class Module(val environment: Environment, val configuration: Configuration) extends AbstractModule with PekkoGuiceSupport:

  override def configure(): Unit = {
    bind(classOf[NrsRetrievalConnector]).to(classOf[NrsRetrievalConnectorImpl])
    bind(classOf[TestOnlyNrsRetrievalConnector]).to(classOf[TestOnlyNrsRetrievalConnectorImpl])
    bind(classOf[Audit]).to(classOf[MicroserviceAudit])
  }

  @Provides
  @Singleton
  def registerNotableEventRefinerProvider(
                                           messagesApi: MessagesApi,
                                           errorPage: error_template
                                    )(using executionContext: ExecutionContext, appConfig: AppConfig): String => NotableEventRefiner =
    new NotableEventRefiner(messagesApi, errorPage)(_)
