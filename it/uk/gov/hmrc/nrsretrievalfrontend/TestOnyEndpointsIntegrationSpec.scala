package uk.gov.hmrc.nrsretrievalfrontend

import play.api.libs.ws.WSResponse

trait TestOnyEndpointsIntegrationSpec extends IntegrationSpec {
  def validateDownloadGetRequest(): WSResponse =
    wsClient.url(s"$serviceRoot/test-only/validate-download").get.futureValue

  def validateDownloadPostRequest(): WSResponse =
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
        validate(validateDownloadGetRequest())
      }
    }
  }

  "POST /nrs-retrieval/test-only/validate-download" should {
    "display the validate-download page" when {
      "the default router is used" in {
        validate(validateDownloadPostRequest())
      }
    }
  }
}

class TestOnyEndpointsDisabledIntegrationSpec extends TestOnyEndpointsIntegrationSpec {
  override val configuration: Map[String, Any] = defaultConfiguration

  "GET /nrs-retrieval/test-only/validate-download" should {
    "return NOT_FOUND" when {
      "the default router is used" in {
        validateDownloadGetRequest().status shouldBe NOT_FOUND
      }
    }
  }

  "POST /nrs-retrieval/test-only/validate-download" should {
    "return NOT_FOUND" when {
      "the default router is used" in {
        validateDownloadPostRequest().status shouldBe NOT_FOUND
      }
    }
  }
}
