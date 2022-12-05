/*
 * Copyright 2022 HM Revenue & Customs
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

import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import com.google.inject.name.Named
import com.typesafe.config.{Config, ConfigFactory}
import connectors.testonly.{TestOnlyNrsRetrievalConnector, TestOnlyNrsRetrievalConnectorImpl}
import connectors.{NrsRetrievalConnector, NrsRetrievalConnectorImpl}
import http.MicroserviceAudit
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.http.ws.WSHttp

import javax.inject.{Inject, Singleton}

class Module(val environment: Environment, val configuration: Configuration) extends AbstractModule with AkkaGuiceSupport {

  override def configure(): Unit = {
    bind(classOf[HttpGet]).to(classOf[HttpVerbs])
    bind(classOf[HttpPost]).to(classOf[HttpVerbs])
    bind(classOf[NrsRetrievalConnector]).to(classOf[NrsRetrievalConnectorImpl])
    bind(classOf[TestOnlyNrsRetrievalConnector]).to(classOf[TestOnlyNrsRetrievalConnectorImpl])
    bind(classOf[Audit]).to(classOf[MicroserviceAudit])
    bind(classOf[HttpClient]).to(classOf[DefaultHttpClient])
  }
}

@Singleton
class HttpVerbs @Inject()(val auditConnector: AuditConnector, @Named("appName") val appName: String, val actorSystem: ActorSystem, val wsClient: WSClient)
  extends HttpGet with HttpPost with HttpPut with HttpPatch with HttpDelete with WSHttp with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  override protected def configuration: Config = ConfigFactory.empty()
}
