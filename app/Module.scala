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

import java.net.URL

import actors.{ActorService, ActorServiceImpl, RetrievalActor}
import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import com.google.inject.name.{Named, Names}
import com.typesafe.config.Config
import config.MicroserviceAudit
import connectors.{MicroAuthConnector, NrsRetrievalConnector, NrsRetrievalConnectorImpl}
import javax.inject.{Inject, Provider, Singleton}
import play.api.Mode.Mode
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

class Module(val environment: Environment, val configuration: Configuration) extends AbstractModule with AkkaGuiceSupport with ServicesConfig {

  val log: Logger = Logger(this.getClass)
  log.info(s"appConfig: Starting service env: ${environment.mode}")

  def configure() = {
    bind(classOf[ActorService]).to(classOf[ActorServiceImpl])
    bindActor[RetrievalActor]("retrieval-actor")

    bind(classOf[HttpGet]).to(classOf[HttpVerbs])
    bind(classOf[HttpPost]).to(classOf[HttpVerbs])
    bind(classOf[NrsRetrievalConnector]).to(classOf[NrsRetrievalConnectorImpl])
    bind(classOf[AuthConnector]).to(classOf[MicroAuthConnector])
    bindBaseUrl("auth")

    bind(classOf[Audit]).to(classOf[MicroserviceAudit])
    bind(classOf[String])
      .annotatedWith(Names.named("appName"))
      .toProvider(new ConfigProvider("appName"))
  }

  override protected def mode: Mode = environment.mode

  override protected def runModeConfiguration: Configuration = configuration

  private class ConfigProvider(confKey: String) extends Provider[String] {
    override lazy val get = configuration.getString(confKey)
      .getOrElse(throw new IllegalStateException(s"No value found for configuration property $confKey"))
  }

  private class BaseUrlProvider(serviceName: String) extends Provider[URL] {
    override lazy val get = new URL(baseUrl(serviceName))
  }

  private def bindBaseUrl(serviceName: String) =
    bind(classOf[URL]).annotatedWith(Names.named(s"$serviceName-baseUrl")).toProvider(new BaseUrlProvider(serviceName))

}

@Singleton
class HttpVerbs @Inject()(val auditConnector: AuditConnector, @Named("appName") val appName: String, val actorSystem: ActorSystem)
  extends HttpGet with HttpPost with HttpPut with HttpPatch with HttpDelete with WSHttp with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  override protected def configuration: Option[Config] = None
}
