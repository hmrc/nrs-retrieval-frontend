package uk.gov.hmrc.nrsretrievalfrontend

import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nrsretrievalfrontend.stubs.NrsRetrievalStubs._

class XApiHeaderIntegrationSpec extends IntegrationSpec {
  private implicit val headerCarrierWithoutXApiKeyHeader: HeaderCarrier = HeaderCarrier()

  override val configuration: Map[String, Any] = defaultConfiguration + ("stride.enabled" -> false)

  private lazy val wsClient = fakeApplication().injector.instanceOf[WSClient]
  private lazy val serviceRoot = s"http://localhost:$port/nrs-retrieval"

  "GET /download/:vaultId/:archiveId" should {
    "pass the X-API-HEADER to the nrs-retrieval backend" in {
      givenGetSubmissionBundlesReturns(OK)
      wsClient.url(s"$serviceRoot/download/$vaultName/$archiveId").get.futureValue.status shouldBe OK
      verifyGetSubmissionBundlesWithXApiKeyHeader()
    }
  }

  "GET /retrieve/:vaultId/:archiveId" should {
    "pass the X-API-HEADER to the nrs-retrieval backend" in {
      givenPostSubmissionBundlesRetrievalRequestsReturns(OK)
      wsClient.url(s"$serviceRoot/retrieve/$vaultName/$archiveId").get.futureValue.status shouldBe ACCEPTED
      verifyPostSubmissionBundlesRetrievalRequestsWithXApiKeyHeader()
    }
  }

  "POST /retrieve/search/:notableEventType" should {
    "pass the X-API-HEADER to the nrs-retrieval backend" in {
      givenSearchReturns(OK)

      wsClient.url(s"$serviceRoot/search/$notableEventType")
        .post(
          Map[String, Seq[String]](
            searchKeyName -> Seq(searchKeyName),
            searchKeyValue -> Seq(searchKeyValue),
            notableEventType -> Seq(notableEventType)
          )
        )
        .futureValue.status shouldBe OK

      verifySearchWithXApiKeyHeader()
    }
  }
}
