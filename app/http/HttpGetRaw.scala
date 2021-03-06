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

package http

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.HttpVerbs.{GET => GET_VERB}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.http.logging.ConnectionTracing
import uk.gov.hmrc.play.http.ws.{WSHttpResponse, WSRequest}

import scala.concurrent.{ExecutionContext, Future}

trait GetRawHttpTransport {
  def doGetRaw(url: String)(implicit hc: HeaderCarrier): Future[WSResponse]
}

trait CoreGetRaw {
  def GETRaw(url: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[WSResponse]
}

trait WSGetRaw extends WSRequest with CoreGetRaw with GetRawHttpTransport {
  override def doGetRaw(url: String)(implicit hc: HeaderCarrier): Future[WSResponse] = buildRequest(url).get()
}

trait HttpGetRaw extends CoreGetRaw with GetRawHttpTransport with HttpVerb with ConnectionTracing with HttpHooks {
  override def GETRaw(url: String)(implicit  hc: HeaderCarrier, ec: ExecutionContext): Future[WSResponse] =
    withTracing(GET_VERB, url) {
      val httpResponse = doGetRaw(url)
      val wsHttpResponse = httpResponse.map(WSHttpResponse(_))

      executeHooks(url, GET_VERB, None, wsHttpResponse)
      mapErrors(GET_VERB, url, wsHttpResponse)
      httpResponse
    }
}
