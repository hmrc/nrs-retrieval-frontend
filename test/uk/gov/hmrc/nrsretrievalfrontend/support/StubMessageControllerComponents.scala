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

package uk.gov.hmrc.nrsretrievalfrontend.support

import org.apache.pekko.stream.testkit.NoMaterializer
import play.api.http.{DefaultFileMimeTypes, FileMimeTypesConfiguration, HttpConfiguration}
import play.api.i18n.Messages.UrlMessageSource
import play.api.i18n.*
import play.api.mvc.*
import play.api.test.Helpers.{stubBodyParser, stubPlayBodyParsers}

import java.util.Locale
import scala.concurrent.ExecutionContext

trait StubMessageControllerComponents extends Configs:

  val lang = Lang(new Locale("en"))

  val langs: Langs = new DefaultLangs(Seq(lang))

  val httpConfiguration = new HttpConfiguration()

  given messages: Map[String, String] =
    Messages
      .parse(UrlMessageSource(this.getClass.getClassLoader.getResource("messages")), "")
      .toOption
      .getOrElse(Map.empty[String, String])

  given messagesApi: MessagesApi =
    new DefaultMessagesApi(
      messages = Map("default" -> messages),
      langs = langs
    )

  given messagesImpl: MessagesImpl = MessagesImpl(lang, messagesApi)

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
