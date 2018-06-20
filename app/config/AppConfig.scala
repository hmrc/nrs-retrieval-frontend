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

package config

import javax.inject.{Inject, Singleton}
import models.{NotableEvent, Service, ServiceScope, SubmissionType}
import org.joda.time.LocalDate
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.collection.JavaConversions._

import scala.concurrent.duration._


@Singleton
class AppConfig @Inject()(val runModeConfiguration: Configuration, val environment: Environment) extends ServicesConfig {
  override protected def mode: Mode = environment.mode

  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private def loadFromConfig(config: Configuration, key: String) = config.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private def loadConfigWithDefault(key: String, default: String) = runModeConfiguration.getString(key).getOrElse(default)

  private val contactHost = runModeConfiguration.getString(s"contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = "MyService"

  lazy val assetsPrefix: String = loadConfig(s"assets.url") + loadConfig(s"assets.version")
  lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  lazy val analyticsHost: String = loadConfig(s"google-analytics.host")
  lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val nrsRetrievalUrl = s"${baseUrl("nrs-retrieval")}/nrs-retrieval"
  lazy val xApiKey: String = loadConfigWithDefault(s"microservice.services.nrs-retrieval.xApiKey", "missingKey")

  lazy val isLocal: Boolean = loadConfigWithDefault(s"microservice.services.nrs-retrieval.isLocal", "false").toBoolean

  lazy val interval: FiniteDuration = loadConfig(s"polling.interval").toLong.millis
  lazy val runTimeMillis: Long = loadConfig(s"polling.duration").toLong

  lazy val futureTimeoutSeconds = 30

  private val vatService = Service("Value Added Tax (VAT)", Seq(
    SubmissionType("VAT Return", "VAT Registration Number (VRN)", LocalDate.parse("2018-04-01"), 20)))

  val serviceScope = ServiceScope(Seq(vatService))

  val notableEvents: Map[String, NotableEvent] =
    runModeConfiguration.getConfigList(s"notableEvents").getOrElse(throw new Exception(s"Missing configuration"))
      .map { client =>
          NotableEvent(
            loadFromConfig(client, "notableEvent"),
            loadFromConfig(client, "displayName"),
            loadFromConfig(client, "storedFrom"),
            loadFromConfig(client, "storedFor"),
            loadFromConfig(client, "searchKeyLabel")
          )
        }.map(nE => nE.name -> nE).toMap

  // todo : this to be replaced on integration with STRIDE
  val userName = "Susan Smith"
  
  lazy val nrsStrideRole: String = loadConfig("stride.role.name")
  lazy val strideAuth: Boolean = runModeConfiguration.getBoolean("stride.enabled").getOrElse(false)
  lazy val authHost: String = runModeConfiguration.getString(s"microservice.services.auth.host").getOrElse("none-authHost")
  lazy val authPort: Int = runModeConfiguration.getInt(s"microservice.services.auth.port").getOrElse(-1)

}