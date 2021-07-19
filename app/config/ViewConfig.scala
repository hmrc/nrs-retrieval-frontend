/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.Inject
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL

case class ViewConfig(
                       appName:                       String,
                       assetsPrefix:                  String,
                       authUrl:                       String,
                       loginUrl:                      String,
                       signOut:                       String,
                       frontendBaseUrl:               FrontendBaseUrlProvider,
                       btaBaseUrl:                    String,
                       viewAndChangeBaseUrl:          String,
                       nddsBaseUrl:                   String,
                       payFrontendBaseUrl:            String,
                       govUkHomeUrl:                  String,
                       accessibilityStatementBaseUrl: String,
                       timeoutDialogTimeout:          Int,
                       timeoutDialogCountdown:        Int
                     ) {

  /*
    * Everything what depends on the `frontendBaseUrl` has to be lazy. Otherwise it will be impossible to run tests
    * based on GuiceOneServerPerSuite because `port` at which the test app is served
    * (which is needed to override `frontendBaseUrl`)
    * is initialized during application start so you can not refer to it before application is started
    * and therefore override it in test configs.
    */
  lazy val reportAProblemPartialUrl = s"${frontendBaseUrl.baseUrl()}/contact/problem_reports_ajax?service=$appName"
  lazy val reportAProblemNonJSUrl = s"${frontendBaseUrl.baseUrl()}/contact/problem_reports_nonjs?service=$appName"
  lazy val reportAccessibilityProblemUrl = s"${frontendBaseUrl.baseUrl()}/contact/accessibility?service=$appName"
  lazy val feedbackUrl = s"${frontendBaseUrl.baseUrl()}/contact/beta-feedback?service=$appName"
  lazy val btaHomeUrl: String = s"$btaBaseUrl/business-account"
  lazy val nddsUrl = s"$nddsBaseUrl/directdebits"
  lazy val host: String = new URL(frontendBaseUrl.baseUrl()).getHost
  val govUkCdsSetupGuidancePageUrl: String = s"$govUkHomeUrl/guidance/set-up-a-direct-debit-for-a-duty-deferment-account-on-the-customs-declaration-service"

  lazy val loginBaseUrl: String = frontendBaseUrl.baseUrl()

  val chooseAWayToPayUrl = s"$payFrontendBaseUrl/pay/vat/choose-a-way-to-pay"

  @Inject
  def this(
            servicesConfig:  ServicesConfig,
            frontendBaseUrl: FrontendBaseUrlProvider
          ) = this(
    appName                       = servicesConfig.getString("appName"),
    assetsPrefix                  = servicesConfig.getString(s"assets.url") + servicesConfig.getString(s"assets.version"),
    authUrl                       = servicesConfig.baseUrl("auth"),
    loginUrl                      = servicesConfig.getString("urls.login"),
    signOut                       = servicesConfig.getString("urls.logout"),
    frontendBaseUrl               = frontendBaseUrl,
    btaBaseUrl                    = servicesConfig.getString("btaBaseUrl"),
    viewAndChangeBaseUrl          = servicesConfig.getString("viewAndChangeBaseUrl"),
    nddsBaseUrl                   = servicesConfig.getString("nddsBaseUrl"),
    payFrontendBaseUrl            = servicesConfig.getString("payFrontendBaseUrl"),
    govUkHomeUrl                  = servicesConfig.getString("govUkHomeUrl"),
    timeoutDialogTimeout          = servicesConfig.getInt("timeout-dialog.timeout"),
    timeoutDialogCountdown        = servicesConfig.getInt("timeout-dialog.countdown"),
    accessibilityStatementBaseUrl = servicesConfig.getString("accessibility-statement-frontend.url")
  )

  val cookiesUrl: String = "https://www.tax.service.gov.uk/help/cookies"
  val privacyNoticeUrl: String = "https://www.tax.service.gov.uk/help/privacy"
  val termsAndConditionsUrl: String = "https://www.tax.service.gov.uk/help/terms-and-conditions"
  val helpUsingGovUkUrl: String = "https://www.gov.uk/help"

  def accessibilityStatementUrl(relativeUrl: String): String = s"$accessibilityStatementBaseUrl/accessibility-statement/direct-debit?referrerUrl=$payFrontendBaseUrl$relativeUrl"
}
