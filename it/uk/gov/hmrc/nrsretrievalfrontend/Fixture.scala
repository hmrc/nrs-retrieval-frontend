package uk.gov.hmrc.nrsretrievalfrontend

import models.SearchQuery

trait Fixture {
  val vatReturn = "vat-return"
  val vrn = "vrn"
  val validVrn = "validVrn"

  val notableEventType = "notableEventType"
  val searchKeyName = "searchKeyName_0"
  val searchKeyValue = "searchKeyValue_0"

  val submissionId = "604958ae-973a-4554-9e4b-fed3025dd845"
  val datetime = "Tue, 13 Jul 2021 12:36:51 GMT"

  val xApiKeyHeader = "X-API-Key"
  val xApiKey = "validKey"

  val vatRegistration = "vat-registration"
  val vatRegistrationSearchKey = "postCodeOrFormBundleId"
  val postCode = "aPostCode"

  val vatReturnSearchQuery: SearchQuery = SearchQuery(Some(vrn), Some(validVrn), vatReturn)
  val vatReturnSearchText: String = vatReturnSearchQuery.searchText(false)

  val vatRegistrationSearchQuery: SearchQuery = SearchQuery(Some(vatRegistrationSearchKey), Some(postCode), vatRegistration)
  val vatRegistrationSearchText: String = vatRegistrationSearchQuery.searchText(true)
}
