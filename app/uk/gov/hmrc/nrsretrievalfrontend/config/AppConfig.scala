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

import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.nrsretrievalfrontend.models.{NotableEvent, SearchKey}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

@Singleton
class AppConfig @Inject()(val runModeConfiguration: Configuration, val environment: Environment, val servicesConfig: ServicesConfig) {

  val logger = Logger(this.getClass.getName)

  private def loadConfig(key: String) = runModeConfiguration.getOptional[String](key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private def loadFromConfig(config: Configuration, key: String) =
    config.getOptional[String](key).getOrElse(throw new Exception(s"Missing configuration key: $key")).replace("_", " ")

  private def loadConfigWithDefault(key: String, default: String) = runModeConfiguration.getOptional[String](key).getOrElse(default)

  lazy val nrsRetrievalUrl = s"${servicesConfig.baseUrl("nrs-retrieval")}/nrs-retrieval"
  lazy val xApiKey: String = loadConfigWithDefault(s"microservice.services.nrs-retrieval.xApiKey", "missingKey")

  lazy val isLocal: Boolean = loadConfigWithDefault(s"microservice.services.nrs-retrieval.isLocal", "false").toBoolean

  lazy val interval: FiniteDuration = loadConfig(s"polling.interval").toLong.millis
  lazy val runTimeMillis: Long = loadConfig(s"polling.duration").toLong

  val testConfig: Option[String] = runModeConfiguration.getOptional[String]("microservice.services.nrs-service.x-api-key")

  logger.info(s"test configuration is = $testConfig")

  lazy val futureTimeoutSeconds = 30

  lazy val notableEvents: Map[String, NotableEvent] =
    runModeConfiguration.underlying.getConfigList(s"notableEvents").asScala.map { clientConfig =>
      val clientConfiguration = Configuration(clientConfig)

      NotableEvent(
        name = loadFromConfig(clientConfiguration, "notableEvent"),
        displayName = loadFromConfig(clientConfiguration, "displayName"),
        pluralDisplayName = loadFromConfig(clientConfiguration, "pluralDisplayName"),
        storedFrom = loadFromConfig(clientConfiguration, "storedFrom"),
        storedFor = loadFromConfig(clientConfiguration, "storedFor"),
        searchKeys = clientConfig.getConfigList("searchKeys").asScala.toList.map { searchKeyConfig =>
          val searchKeyConfiguration = Configuration(searchKeyConfig)

          SearchKey(
            name = loadFromConfig(searchKeyConfiguration, "name"),
            label = loadFromConfig(searchKeyConfiguration, "label"))
        },
        estimatedRetrievalTime = clientConfiguration.getOptional[FiniteDuration]("estimatedRetrievalTime").getOrElse(5.minutes),
        crossKeySearch = clientConfiguration.getOptional[String]("crossKeySearch").getOrElse("") == "true"
      )
    }.map(nE => nE.name -> nE).toMap

}