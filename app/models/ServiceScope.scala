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

package models

import org.joda.time.LocalDate
import play.api.{ConfigLoader, Configuration}
import scala.language.implicitConversions

case class ServiceScope (services: Seq[Service])

case class Service (taxService: String, submissionTypes: Seq[SubmissionType])

case class SubmissionType (description: String, searchBy: String, storedFrom: LocalDate, yearsStored: Int)


final case class MDTPService(host: String, port: String, protocol: String) {

  def baseUrl: String = s"$protocol://$host:$port"

  override def toString: String = baseUrl
}

object MDTPService {

  implicit lazy val configLoader: ConfigLoader[MDTPService] = ConfigLoader {
    config =>
      prefix =>

        val service  = Configuration(config).get[Configuration](prefix)
        val host     = service.get[String]("host")
        val port     = service.get[String]("port")
        val protocol = service.get[String]("protocol")

        MDTPService(host, port, protocol)
  }

  implicit def convertToString(service: MDTPService): String = service.baseUrl
}