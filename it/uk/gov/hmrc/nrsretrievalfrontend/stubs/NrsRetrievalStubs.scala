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
package uk.gov.hmrc.nrsretrievalfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, stubFor, urlEqualTo, _}
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object NrsRetrievalStubs {
//  def givenNrsSearchResultInit(callbackUrl: String): StubMapping =
//    stubFor(
//      post(urlEqualTo(s"/api/v2/init"))
//        .withRequestBody(equalToJson(
//          s"""
//             |{[
//             |  {
//             |    "businessId": "VAT",
//             |    "notableEvent": "vat-return",
//             |    "payloadContentType": "application/json",
//             |    "userSubmissionTimestamp": "2018-04-07T12:13:25.156Z",
//             |    "identityData": {
//             |      "internalId": "some-id",
//             |      "externalId": "some-id",
//             |      "agentCode": "TZRXXV",
//             |      <snip>
//             |    },
//             |    "userAuthToken": "AbCdEf123456",
//             |    "headerData": {
//             |      "Gov-Client-Public-IP": "198.51.100.0",
//             |      "Gov-Client-Public-Port": "12345",
//             |      <snip>
//             |    },
//             |    "searchKeys": {
//             |      "vrn": "123456789",
//             |      "taxPeriodEndDate": "2018-04-05"
//             |    },
//             |    "nrSubmissionId": "beb6f5b8-f2c3-4d5a-8c72-0888fc1bbbfd",
//             |    "payloadMessageDigest": "0ec61e556ae5087fec39b43cd45ee0dbd9e9b782a9b0a3ae6c9078406b42e6db",
//             |    "signatureTimestamp": "2018-05-03T09:37:45Z",
//             |    "submissionApiKeyDigest": "83ff85b1f17f13277e821b86c644f1a42f2ac024629ad0ee987170f760cb4c",
//             |    "payloadMessageDigestType": "SHA-256",
//             |    "bundle":{
//             |      "fileType": "zip",
//             |      "fileSize": "123456"
//             |    },
//             |    "glacier": {
//             |      "vaultName": "vat-return-2018",
//             |      "archiveId": "29381782738273"
//             |    },
//             |    "expiryDate": "2038-04-07"
//             | },
//             |
//             |
//             |  {
//             |    "businessId": "VAT",
//             |    "notableEvent": "VAT Return",
//             |    "payloadContentType": "application/json",
//             |    "userSubmissionTimestamp": "2018-04-07T12:13:25.156Z",
//             |    "identityData": {
//             |      "internalId": "some-id",
//             |      "externalId": "some-id",
//             |      "agentCode": "TZRXXV",
//             |      <snip>
//             |    },
//             |    "userAuthToken": "AbCdEf123456",
//             |    "headerData": {
//             |      "Gov-Client-Public-IP": "198.51.100.0",
//             |      "Gov-Client-Public-Port": "12345",
//             |      <snip>
//             |    },
//             |    "searchKeys": {
//             |      "vrn": "123456789",
//             |      "taxPeriodEndDate": "2018-04-05"
//             |    },
//             |    "nrSubmissionId": "beb6f5b8-f2c3-4d5a-8c72-0888fc1bbbfd",
//             |    "payloadMessageDigest": "0ec61e556ae5087fec39b43cd45ee0dbd9e9b782a9b0a3ae6c9078406b42e6db",
//             |    "signatureTimestamp": "2018-05-03T09:37:45Z",
//             |    "submissionApiKeyDigest": "83ff85b1f17f13277e821b86c644f1a42f2ac024629ad0ee987170f760cb4c",
//             |    "payloadMessageDigestType": "SHA-256",
//             |    "bundle":{
//             |      "fileType": "zip",
//             |      "fileSize": "123456"
//             |    },
//             |    "glacier": {
//             |      "vaultName": "vat-return-2018",
//             |      "archiveId": "29381782738273"
//             |    },
//             |    "expiryDate": "2038-04-07"
//             | },
//             |""".stripMargin)
//        )
//        .willReturn(
//          aResponse()
//            .withStatus(202)
//            .withHeader(HeaderNames.???, callbackUrl)))

  def givenHeadSubmissionBundlesReturns(status: Int): StubMapping =
    stubFor(
      head(urlEqualTo(s"/nrs-retrieval/submission-bundles/vaultName/archiveId"))
        .willReturn(aResponse().withStatus(status)))

            //.withHeader(HeaderNames.???, s"/get-results/nrs-retrieval/submission-metadata?")))

//  def givenNrsSearchResult(server: WireMockServer): StubMapping =
//    stubFor(
//      get(urlEqualTo(s"test/v1/api"))
//        .willReturn(
//          aResponse()
//            .withStatus(404)
//            .withHeader(HeaderNames.???, s"/get-results/nrs-retrieval/submission-metadata?")))

//  def givenNrsSearchKeys(vrn: String, taxPeriodEndDate: String, userAuthToken: String
//    stubFor(
//      get(urlEqualTo(testUrl = s"/api/confirmed?id=$vrn"))
//        .willReturn(
//          aResponse()
//            .withStatus(200)
//            .withBody(s"""
//              {
//                 |    "businessId": "VAT" {
//                 |    "notableEvent": "VAT Return" {
//                 |    "payloadContentType": "application/json",
//                 |    "userSubmissionTimestamp": "2018-04-07T12:13:25.156Z",
//                 |    "identityData": {
//                 |    "internalId": "some-id",
//                 |     },
//                 |    "searchKeys": [
//                 |            "$vrn",
//                 |            "$taxPeriodEndDate",
//                 |        ],
//                 |        "userAuthToken": "AbCdEf123456",
//                 |    },
//                 |    "nrSubmissionId": "beb6f5b8-f2c3-4d5a-8c72-0888fc1bbbfd",
//                 |    "id": "GB990091234524"
//                 |}
//                 |""".stripMargin)))
}