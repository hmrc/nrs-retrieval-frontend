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

package controllers.testonly

import connectors.testonly.TestOnlyNrsRetrievalConnector
import controllers.ControllerSpec
import controllers.testonly.FormMappings.{archiveId, vaultName}
import models.testonly.ValidateDownloadResult
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}

import scala.concurrent.Future

class ValidateDownloadControllerSpec extends ControllerSpec {
  private val connector = mock[TestOnlyNrsRetrievalConnector]

  private lazy val controller =
    new ValidateDownloadController(
      stubMessagesControllerComponents(),
      mockAuthConnector,
      error_template,
      connector,
      injector.instanceOf[views.html.testonly.validate_download_page]
    )

  private def validateResponse(eventualResult: Future[Result]) = {
    val content = Jsoup.parse(contentAsString(eventualResult))

    status(eventualResult) shouldBe OK
    content.getElementById("pageHeader").text() shouldBe "test-only.validate-download.page.header"
    content.getElementById("vaultName").tag().getName shouldBe "input"
    content.getElementById("archiveId").tag().getName shouldBe "input"
    content.getElementsByAttributeValue("name", "submitButton").text() shouldBe "test-only.validate-download.form.submit"

    val form = content.getElementsByClass("form")
    form.attr("action") shouldBe routes.ValidateDownloadController.submitValidateDownload().url
    form.attr("method") shouldBe "POST"

    content
  }

  "showValidateDownload" should {
    "display the validate download page" in {
      validateResponse(controller.showValidateDownload(getRequest))
    }
  }

  "submitValidateDownload" should {
    "display the validate download page" in {
      val aVaultName = "vaultName"
      val anArchiveId = "archiveId"
      val zipSize = 10000
      val file1 = "file1"
      val file2 = "file2"
      val files = Seq(file1, file2)
      val header1 = ("header1", "h1")
      val header2 = ("header2", "h2")

      when(connector.validateDownload(Matchers.eq(aVaultName), Matchers.eq(anArchiveId), any())(any()))
        .thenReturn(Future successful ValidateDownloadResult(OK, zipSize, files, Seq(header1, header2)))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        emptyPostRequest.withFormUrlEncodedBody((vaultName, aVaultName), (archiveId, anArchiveId))

      val eventualResult = controller.submitValidateDownload(request)
      val content = validateResponse(eventualResult)

      content.getElementById("resultsHeader").text() shouldBe "test-only.validate-download.results.title"
      content.getElementById("status").text() shouldBe OK.toString
      content.getElementById("numberOfZippedFiles").text() shouldBe files.size.toString
      content.getElementById(file1).text() shouldBe file1
      content.getElementById(file2).text() shouldBe file2
      content.getElementById(header1._1).text() shouldBe header1._2
      content.getElementById(header2._1).text() shouldBe header2._2
    }
  }
}
