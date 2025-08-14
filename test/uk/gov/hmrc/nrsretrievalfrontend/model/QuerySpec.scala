/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.nrsretrievalfrontend.model

import play.api.libs.json.Json
import uk.gov.hmrc.nrsretrievalfrontend.support.UnitSpec
import uk.gov.hmrc.nrsretrievalfrontend.models.Query

class QuerySpec extends UnitSpec:

  "createJsonQuery" should :
    "create a json query with 1 value" in :
      val q1 = Query("nino", "123")
      val queries: List[Query] = List(q1)
      val jsonString = Query.createJsonQuery("itsa-ad-hoc-refund", queries)
      val json = Json.parse(jsonString)

      Json.prettyPrint(json) shouldBe
        """{
          |  "notableEvent" : "itsa-ad-hoc-refund",
          |  "query" : {
          |    "key" : "nino",
          |    "value" : "123"
          |  }
          |}""".stripMargin

    "create a json query with 2 value" in :
      val q1 = Query("nino", "123")
      val q2 = Query("saUtr", "456")
      val queries: List[Query] = List(q1, q2)
      val jsonString = Query.createJsonQuery("itsa-ad-hoc-refund", queries)
      val json = Json.parse(jsonString)
      Json.prettyPrint(json) shouldBe
        """{
          |  "notableEvent" : "itsa-ad-hoc-refund",
          |  "query" : {
          |    "type" : "or",
          |    "q1" : {
          |      "key" : "saUtr",
          |      "value" : "456"
          |    },
          |    "q2" : {
          |      "key" : "nino",
          |      "value" : "123"
          |    }
          |  }
          |}""".stripMargin

    "create a json query with 3 value" in :
      val q1 = Query("nino", "123")
      val q2 = Query("saUtr", "456")
      val q3 = Query("providerId", "789")
      val queries: List[Query] = List(q1, q2, q3)
      val jsonString = Query.createJsonQuery("itsa-ad-hoc-refund", queries)
      val json = Json.parse(jsonString)
      Json.prettyPrint(json) shouldBe
        """{
          |  "notableEvent" : "itsa-ad-hoc-refund",
          |  "query" : {
          |    "type" : "or",
          |    "q1" : {
          |      "key" : "providerId",
          |      "value" : "789"
          |    },
          |    "q2" : {
          |      "type" : "or",
          |      "q1" : {
          |        "key" : "saUtr",
          |        "value" : "456"
          |      },
          |      "q2" : {
          |        "key" : "nino",
          |        "value" : "123"
          |      }
          |    }
          |  }
          |}""".stripMargin
