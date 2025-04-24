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

package uk.gov.hmrc.nrsretrievalfrontend.models.testonly

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.util.ByteString
import play.api.http.HeaderNames
import uk.gov.hmrc.http.HttpResponse

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import scala.concurrent.{ExecutionContext, Future}

case class ValidateDownloadResult(
  status: Int,
  zipSize: Long,
  files: Seq[String],
  headers: Seq[(String, String)]
)
object ValidateDownloadResult extends HeaderNames:
  def apply(
    response: HttpResponse
  )(using ec: ExecutionContext): Future[ValidateDownloadResult] =
    given ActorSystem = ActorSystem()

    response.bodyAsSource.runFold(ByteString.emptyByteString)(_ ++ _).map { bytes =>
      val headers: Seq[(String, String)] = response.headers.keys.map { key =>
        (key, response.headers(key).head)
      }.toSeq

      val zis: ZipInputStream =
        new ZipInputStream(new ByteArrayInputStream(bytes.toArray))

      val zippedFileNames: Seq[String] = LazyList
        .continually(zis.getNextEntry)
        .takeWhile(_ != null)
        .map(_.getName)

      ValidateDownloadResult(
        response.status,
        bytes.size,
        zippedFileNames,
        headers
      )
    }
