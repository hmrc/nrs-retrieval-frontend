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

package support

import akka.stream.testkit.NoMaterializer
import com.typesafe.config.ConfigFactory
import config.AppConfig
import connectors.NrsRetrievalConnector
import models.NotableEvent
import org.scalatest.Suite
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.{DefaultFileMimeTypes, FileMimeTypesConfiguration, HttpConfiguration}
import play.api.i18n.Messages.UrlMessageSource
import play.api.i18n.{DefaultLangs, DefaultMessagesApi, Lang, Langs, Messages, MessagesApi, MessagesImpl}
import play.api.inject.{Injector, bind}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.typedmap.TypedKey
import play.api.mvc.{AnyContentAsEmpty, DefaultActionBuilder, DefaultMessagesActionBuilderImpl, DefaultMessagesControllerComponents, MessagesControllerComponents, Request}
import play.api.test.FakeRequest
import play.api.{Application, Configuration, Environment, Mode}
import play.filters.csrf.{CSRFConfigProvider, CSRFFilter}

import scala.collection.immutable
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.Helpers.{stubBodyParser, stubPlayBodyParsers}
import uk.gov.hmrc.govukfrontend.views.html.components.{GovukBackLink, GovukFooter, GovukHeader, GovukLayout, GovukSkipLink, GovukTemplate, TwoThirdsMainContent}
import uk.gov.hmrc.hmrcfrontend.config.{AccessibilityStatementConfig, AssetsConfig, LanguageConfig, TrackingConsentConfig}
import uk.gov.hmrc.hmrcfrontend.views.config.HmrcFooterItems
import uk.gov.hmrc.hmrcfrontend.views.html.components.{HmrcFooter, HmrcInternalHeader, HmrcLanguageSelect}
import uk.gov.hmrc.hmrcfrontend.views.html.helpers.{HmrcHead, HmrcLanguageSelectHelper, HmrcScripts, HmrcStandardFooter, HmrcTrackingConsentSnippet}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import views.html.components.{Heading1, Layout}

import java.util.Locale
import scala.concurrent.ExecutionContext

class GuiceAppSpec extends UnitSpec with StubMessageControllerComponents with PatienceConfiguration { this: Suite =>
  val nrsRetrievalConnector: NrsRetrievalConnector = mock[NrsRetrievalConnector]

  lazy val indexedNotableEvents: immutable.Seq[(NotableEvent, Int)] =
    appConfig.notableEvents.values.toList.sortBy(_.displayName).zipWithIndex

  implicit val executionContext = scala.concurrent.ExecutionContext.global

  def addToken[T](fakeRequest: FakeRequest[T]): Request[T] = {

    fakeRequest.withCSRFToken
  }

  val layout = new Layout(
    new GovukLayout(
      new GovukTemplate(
        new GovukHeader,
        new GovukFooter,
        new GovukSkipLink
      ),
      new GovukHeader,
      new GovukFooter,
      new GovukBackLink,
      new TwoThirdsMainContent
    ),
    new HmrcInternalHeader(),
    new HmrcStandardFooter(new HmrcFooter, new HmrcFooterItems(new AccessibilityStatementConfig(configuration))),
    new HmrcHead(new HmrcTrackingConsentSnippet(new TrackingConsentConfig(configuration)), new AssetsConfig),
    new HmrcScripts(new AssetsConfig),
    new TwoThirdsMainContent(),
    new Heading1(),
    new GovukBackLink()
  )
}

trait StubMessageControllerComponents extends Configs {

  val lang = Lang(new Locale("en"))

  val langs: Langs = new DefaultLangs(Seq(lang))

  val httpConfiguration = new HttpConfiguration()

  implicit val messages: Map[String, String] =
    Messages
      .parse(UrlMessageSource(this.getClass.getClassLoader.getResource("messages")), "")
      .toOption
      .getOrElse(Map.empty[String, String])

  implicit lazy val messagesApi: MessagesApi =
    new DefaultMessagesApi(
      messages = Map("default" -> messages),
      langs = langs
    )

  implicit val messagesImpl: MessagesImpl = MessagesImpl(lang, messagesApi)

  def stubMessagesControllerComponents()(implicit
                                         executionContext: ExecutionContext
  ): MessagesControllerComponents =
    DefaultMessagesControllerComponents(
      new DefaultMessagesActionBuilderImpl(stubBodyParser(AnyContentAsEmpty), messagesApi),
      DefaultActionBuilder(stubBodyParser(AnyContentAsEmpty)),
      stubPlayBodyParsers(NoMaterializer),
      messagesApi,
      langs,
      new DefaultFileMimeTypes(FileMimeTypesConfiguration()),
      executionContext
    )

}

trait Configs {

  def configuration: Configuration = Configuration(ConfigFactory.parseResources("application.conf"))

  def environment: Environment = Environment.simple()

  def servicesConfig = new ServicesConfig(configuration)

  implicit def appConfig: AppConfig = new AppConfig(configuration, environment, servicesConfig)
}
