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

package config

import javax.inject.{Inject, Singleton}
import models._
import org.joda.time.LocalDate
import play.Logger
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.collection.JavaConverters._
import scala.concurrent.duration._


@Singleton
class AppConfig @Inject()(val runModeConfiguration: Configuration, val environment: Environment, servicesConfig: ServicesConfig) {

  private def loadConfig(key: String) = runModeConfiguration.getOptional[String](key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private def loadFromConfig(config: Configuration, key: String) =
    config.getOptional[String](key).getOrElse(throw new Exception(s"Missing configuration key: $key")).replace("_", " ")

  private def loadConfigWithDefault(key: String, default: String) = runModeConfiguration.getOptional[String](key).getOrElse(default)

  private val contactHost = runModeConfiguration.getOptional[String](s"contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = "MyService"

  lazy val authUrl: String = servicesConfig.baseUrl("auth")

  lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  lazy val analyticsHost: String = loadConfig(s"google-analytics.host")
  lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val nrsRetrievalUrl = s"${servicesConfig.baseUrl("nrs-retrieval")}/nrs-retrieval"
  lazy val xApiKey: String = loadConfigWithDefault(s"microservice.services.nrs-retrieval.xApiKey", "missingKey")

  lazy val isLocal: Boolean = loadConfigWithDefault(s"microservice.services.nrs-retrieval.isLocal", "false").toBoolean

  lazy val interval: FiniteDuration = loadConfig(s"polling.interval").toLong.millis
  lazy val runTimeMillis: Long = loadConfig(s"polling.duration").toLong

  lazy val futureTimeoutSeconds = 30

  private val vatService = Service("Value Added Tax (VAT)", Seq(
    SubmissionType("VAT Return", "VAT Registration Number (VRN)", LocalDate.parse("2018-04-01"), 20)))

  val serviceScope: ServiceScope = ServiceScope(Seq(vatService))

  lazy val notableEvents: Map[String, NotableEvent] =
    runModeConfiguration.underlying.getConfigList(s"notableEvents").asScala.map { clientConfig =>
      val clientConfiguration = Configuration(clientConfig)

      NotableEvent(
        name = loadFromConfig(clientConfiguration, "notableEvent"),
        displayName = loadFromConfig(clientConfiguration, "displayName"),
        storedFrom = loadFromConfig(clientConfiguration, "storedFrom"),
        storedFor = loadFromConfig(clientConfiguration, "storedFor"),
        searchKeys = clientConfig.getConfigList("searchKeys").asScala.map { searchKeyConfig =>
          val searchKeyConfiguration = Configuration(searchKeyConfig)

          SearchKey(
            name = loadFromConfig(searchKeyConfiguration, "name"),
            label = loadFromConfig(searchKeyConfiguration, "label"))
        },
        estimatedRetrievalTime = clientConfiguration.getOptional[FiniteDuration]("estimatedRetrievalTime").getOrElse(5.minutes),
        crossKeySearch = clientConfiguration.getOptional[String]("crossKeySearch").getOrElse("") == "true"
      )
    }.map(nE => nE.name -> nE).toMap

  Logger.of(classOf[AppConfig]).info(s"Notable events available $notableEvents")

  lazy val authHost: String = runModeConfiguration.getOptional[String](s"microservice.services.auth.host").getOrElse("none-authHost")
  lazy val authPort: Int = runModeConfiguration.getOptional[Int](s"microservice.services.auth.port").getOrElse(-1)
}