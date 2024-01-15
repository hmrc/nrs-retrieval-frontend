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

import akka.stream.Materializer
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import org.apache.commons.io.IOUtils
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.HttpResponse

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import scala.util.Try

case class ValidateDownloadResult(status: Int, zipSize: Long, files: Seq[String], headers: Seq[(String,String)] )

object ValidateDownloadResult extends HeaderNames{
  def apply(response: WSResponse): ValidateDownloadResult = {
    val bytes = response.bodyAsBytes

    val headers: Seq[(String, String)] = response.headers.keys.map{ key =>
      (key, response.headers(key).head)
    }.toSeq

    val zis: ZipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes.toArray))

    val zippedFileNames: Seq[String] = LazyList.continually(zis.getNextEntry).takeWhile(_ != null).map(_.getName)

    ValidateDownloadResult(response.status, bytes.size, zippedFileNames, headers)
  }

  def apply(response: HttpResponse)(implicit materializer: Materializer): ValidateDownloadResult = {
    val bytes: Source[ByteString, _] = response.bodyAsSource

    val sink = StreamConverters.asInputStream()

    val inputStream = bytes.runWith(sink)

    val optContentLength =
      for {
        header           <- response.headers.get("content-length")
        value            <- header.headOption
        optContentLength <- Try(value.toLong).toOption
      } yield optContentLength

    val headers: Seq[(String, String)] = response.headers.keys.map { key =>
      (key, response.headers(key).head)
    }.toSeq

    val zis: ZipInputStream = new ZipInputStream(inputStream)

    val contentLength = optContentLength.getOrElse(IOUtils.toByteArray(zis).length.toLong)

    val zippedFileNames: Seq[String] = LazyList.continually(zis.getNextEntry).takeWhile(_ != null).map(_.getName)

    ValidateDownloadResult(response.status, contentLength, zippedFileNames, headers)
  }
}
