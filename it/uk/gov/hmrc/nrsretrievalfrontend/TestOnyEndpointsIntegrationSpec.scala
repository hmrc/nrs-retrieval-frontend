package uk.gov.hmrc.nrsretrievalfrontend

import play.api.libs.ws.WSResponse

trait TestOnyEndpointsIntegrationSpec extends IntegrationSpec {
  def getValidateDownloadRequest(): WSResponse =
    wsClient.url(s"$serviceRoot/test-only/validate-download").get.futureValue

  def postValidateDownloadRequest(): WSResponse =
    wsClient
      .url(s"$serviceRoot/test-only/validate-download")
      .post(Map("vaultName" -> Seq("vaultName1"), "archiveId" -> Seq("archiveId1"))).futureValue

  override val configuration: Map[String, Any] =
    defaultConfiguration + ("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
}

class TestOnyEndpointsEnabledIntegrationSpec extends TestOnyEndpointsIntegrationSpec {
  private def validate(response: WSResponse) = {
    response.status shouldBe OK
    response.contentType shouldBe "text/html; charset=UTF-8"
    response.body.contains("Test-only validate download") shouldBe true
  }

  "GET /nrs-retrieval/test-only/validate-download" should {
    "display the validate-download page" when {
      "the testOnlyDoNotUseInAppConf router is used" in {
        validate(getValidateDownloadRequest())
      }
    }
  }

  "POST /nrs-retrieval/test-only/validate-download" should {
    "display the validate-download page" when {
      "the default router is used" in {
        validate(postValidateDownloadRequest())
      }
    }
  }
}

class TestOnyEndpointsDisabledIntegrationSpec extends TestOnyEndpointsIntegrationSpec {
  override val configuration: Map[String, Any] = defaultConfiguration

  "GET /nrs-retrieval/test-only/validate-download" should {
    "return NOT_FOUND" when {
      "the default router is used" in {
        getValidateDownloadRequest().status shouldBe NOT_FOUND
      }
    }
  }

  "POST /nrs-retrieval/test-only/validate-download" should {
    "return NOT_FOUND" when {
      "the default router is used" in {
        postValidateDownloadRequest().status shouldBe NOT_FOUND
      }
    }
  }
}
