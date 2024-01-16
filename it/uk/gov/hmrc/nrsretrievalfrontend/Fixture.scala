package uk.gov.hmrc.nrsretrievalfrontend

import uk.gov.hmrc.nrsretrievalfrontend.models.{Query, SearchQueries}

import java.net.URLEncoder

trait Fixture {
  val vatReturn = "vat-return"
  val vrn = "vrn"
  val validVrn = "validVrn"

  val notableEventType = "notableEventType"
  val searchKeyName = "queries[0].name"
  val searchKeyValue = "queries[0].value"

  val submissionId = "604958ae-973a-4554-9e4b-fed3025dd845"
  val datetime = "Tue, 13 Jul 2021 12:36:51 GMT"

  val xApiKeyHeader = "X-API-Key"
  val xApiKey = "validKey"

  val vatRegistration = "vat-registration"
  val vatRegistrationSearchKey = "postCodeOrFormBundleId"
  val postCode = "aPostCode"

  def queryString(query: Seq[(String, String)]): String =
    query.map { case (k, v) => s"$k=${URLEncoder.encode(v, "utf-8")}" }.mkString("", "&", "")

  val vatReturnSearchQuery: SearchQueries = SearchQueries(List(Query(vrn, validVrn)))
  val vatReturnSearchText: String = queryString(Query.queryParams(vatReturn, vatReturnSearchQuery.queries, false))

  val vatRegistrationSearchQuery: SearchQueries = SearchQueries(List(Query(vatRegistrationSearchKey, postCode)))
  val vatRegistrationSearchText: String = queryString(Query.queryParams(vatRegistration, vatRegistrationSearchQuery.queries, true))
}
