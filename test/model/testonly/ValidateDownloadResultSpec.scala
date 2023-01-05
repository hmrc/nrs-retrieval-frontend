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

package model.testonly

import akka.util.ByteString
import models.testonly.ValidateDownloadResult
import org.mockito.Mockito.when
import org.mockito.internal.stubbing.answers.Returns
import play.api.libs.ws.WSResponse
import support.UnitSpec

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset.defaultCharset
import java.util.zip.{ZipEntry, ZipOutputStream}

class ValidateDownloadResultSpec extends UnitSpec {
  private val wsResponse = mock[WSResponse]

  "ValidateDownloadResultSpec.apply" should {
    "transform a WSResponse" in {
      val output: Array[Byte] = "text".getBytes(defaultCharset())
      val byteArrayOutputStream = new ByteArrayOutputStream()
      val zipOutputStream: ZipOutputStream = new ZipOutputStream(byteArrayOutputStream)
      val fileNames = Seq("submission.json", "signed-submission.p7m", "metadata.json", "signed-metadata.p7m")

      fileNames.foreach { fileName =>
        val zipEntry: ZipEntry = new ZipEntry(fileName)
        zipOutputStream.putNextEntry(zipEntry)
        zipOutputStream.write(output)
        zipOutputStream.closeEntry()
      }

      val bytes = ByteString(byteArrayOutputStream.toByteArray)

      when(wsResponse.status).thenReturn(OK)
      when(wsResponse.headers).thenAnswer(new Returns(Map("foo" -> Seq("bar"))))
      when(wsResponse.bodyAsBytes).thenReturn(bytes)

      ValidateDownloadResult(wsResponse) shouldBe ValidateDownloadResult(OK, bytes.length, fileNames, Seq(("foo", "bar")) )

      zipOutputStream.close()
    }
  }
}
