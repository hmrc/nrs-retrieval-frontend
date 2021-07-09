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

package model.testonly

import akka.util.ByteString
import models.testonly.ValidateDownloadResult
import org.mockito.Mockito.when
import org.mockito.internal.stubbing.answers.Returns
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.play.test.UnitSpec

import java.io.File
import java.nio.file.Files.readAllBytes

class ValidateDownloadResultSpec extends UnitSpec with MockitoSugar with Status {
  private val wsResponse = mock[WSResponse]

  "ValidateDownloadResultSpec.apply" should {
    "transform a WSResponse" in {
      val file = new File(getClass.getResource("/resources/d5da7fbc-7a27-4414-b277-6216b76c16e6.zip").getFile)
      val bytes = ByteString(readAllBytes(file.toPath))

      when(wsResponse.status).thenReturn(OK)
      when(wsResponse.headers).thenAnswer(new Returns(Map("foo" -> Seq("bar"))))
      when(wsResponse.bodyAsBytes).thenReturn(bytes)

      ValidateDownloadResult(wsResponse) shouldBe
        ValidateDownloadResult(
          OK,
          bytes.length,
          Seq("submission.json", "submission-signature.asn1", "metadata.json", "metadata-signature.asn1"),
          Seq(("foo", "bar")) )
    }
  }
}
