package uk.gov.hmrc.nrsretrievalfrontend

import play.api.libs.ws.WSResponse

trait TestOnyEndpointsIntegrationSpec extends IntegrationSpec {
  def validateDownloadRequest(): WSResponse =
    wsClient.url(s"$serviceRoot/test-only/validate-download").get.futureValue

  override val configuration: Map[String, Any] =
    defaultConfiguration + ("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
}

class TestOnyEndpointsEnabledIntegrationSpec extends TestOnyEndpointsIntegrationSpec {
  "GET /nrs-retrieval/test-only/validate-download" should {
    "display the validate-download page" when {
      "the testOnlyDoNotUseInAppConf router is used" in {
        val response = validateDownloadRequest()
        response.status shouldBe OK
        response.contentType shouldBe "text/html; charset=UTF-8"
        response.body.contains("Test-only validate download") shouldBe true
      }
    }
  }
}

class TestOnyEndpointsDisabledIntegrationSpec extends TestOnyEndpointsIntegrationSpec {
  override val configuration: Map[String, Any] = defaultConfiguration

  "GET /nrs-retrieval/test-only/validate-download" should {
    "return NOT_FOUND" when {
      "the default router is used" in {
        validateDownloadRequest().status shouldBe NOT_FOUND
      }
    }
  }
}
