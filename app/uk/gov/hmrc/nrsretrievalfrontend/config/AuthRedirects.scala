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

package uk.gov.hmrc.nrsretrievalfrontend.config

import play.api.mvc.Result
import play.api.mvc.Results.*
import play.api.{Configuration, Environment, Mode}

trait AuthRedirects:
  def config: Configuration
  def env: Environment

  private lazy val envPrefix =
    if env.mode.equals(Mode.Test) then "Test"
    else config.getOptional[String]("run.mode").getOrElse("Dev")

  private val hostDefaults: Map[String, String] = Map(
    "Dev.external-url.bas-gateway-frontend.host"           -> "http://localhost:9553",
    "Dev.external-url.citizen-auth-frontend.host"          -> "http://localhost:9029",
    "Dev.external-url.identity-verification-frontend.host" -> "http://localhost:9938",
    "Dev.external-url.stride-auth-frontend.host"           -> "http://localhost:9041"
  )

  private def host(service: String): String =
    val key = s"$envPrefix.external-url.$service.host"
    config.getOptional[String](key).orElse(hostDefaults.get(key)).getOrElse("")

  final lazy val defaultOrigin: String =
    config
      .getOptional[String]("sosOrigin")
      .orElse(config.getOptional[String]("appName"))
      .getOrElse("undefined")
  def origin: String                   = defaultOrigin
  def strideLoginUrl: String           = host("stride-auth-frontend") + "/stride/sign-in"
  def toStrideLogin(
    successUrl: String,
    failureUrl: Option[String] = None
  ): Result =
    Redirect(
      strideLoginUrl,
      Map(
        "successURL" -> Seq(successUrl),
        "origin"     -> Seq(origin)
      ) ++ failureUrl.map(f => Map("failureURL" -> Seq(f))).getOrElse(Map())
    )
