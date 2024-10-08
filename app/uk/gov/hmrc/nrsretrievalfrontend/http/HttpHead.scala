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

package uk.gov.hmrc.nrsretrievalfrontend.http

import uk.gov.hmrc.http.HttpReads.Implicits
import uk.gov.hmrc.http.HttpVerbs.{HEAD => HEAD_VERB}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.{HttpHooks, RequestData, ResponseData}
import uk.gov.hmrc.http.logging.ConnectionTracing
import uk.gov.hmrc.play.http.logging.Mdc
import uk.gov.hmrc.play.http.ws.{WSHttpResponse, WSRequest}

import scala.concurrent.{ExecutionContext, Future}

trait HeadHttpTransport {
  def doHead(url: String, headers: Seq[(String, String)])(implicit executionContext: ExecutionContext): Future[HttpResponse]
}

trait CoreHead {
  def HEAD(url: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]
}

trait WSHead extends WSRequest with CoreHead with HeadHttpTransport {

  override def doHead(url: String, headers: Seq[(String, String)])(implicit executionContext: ExecutionContext): Future[HttpResponse] =
    Mdc.preservingMdc(buildRequest(url, headers).head().map(WSHttpResponse.apply))
}

trait HttpHead extends CoreHead with HeadHttpTransport with HttpVerb with ConnectionTracing with HttpHooks with Retries {

  private lazy val hcConfig = HeaderCarrier.Config.fromConfig(configuration)

  override def HEAD(url: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    withTracing(HEAD_VERB, url) {
      val allHeaders   = HeaderCarrier.headersForUrl(hcConfig, url, headers) :+ "Http-Client-Version" -> BuildInfo.version
      val httpResponse = retryOnSslEngineClosed(HEAD_VERB, url)(doHead(url, headers = allHeaders))

      executeHooks(
        HEAD_VERB,
        url"$url",
        RequestData(allHeaders, None),
        httpResponse.map(ResponseData.fromHttpResponse)
      )

      mapErrors(HEAD_VERB, url, httpResponse).map(response => Implicits.readRaw.read(HEAD_VERB, url, response))
    }
}

