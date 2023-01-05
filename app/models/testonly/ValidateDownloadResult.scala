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

package models.testonly

import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

case class ValidateDownloadResult(status: Int, zipSize: Long, files: Seq[String], headers: Seq[(String,String)] )

object ValidateDownloadResult extends HeaderNames{
  def apply(response: WSResponse): ValidateDownloadResult = {
    val bytes = response.bodyAsBytes

    val headers: Seq[(String, String)] = response.headers.keys.map{ key =>
      (key, response.headers(key).head)
    }.toSeq

    val zis: ZipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes.toArray))

    val zippedFileNames: Seq[String] = Stream.continually(zis.getNextEntry).takeWhile(_ != null).map(_.getName)

    ValidateDownloadResult(response.status, bytes.size, zippedFileNames, headers)
  }
}
