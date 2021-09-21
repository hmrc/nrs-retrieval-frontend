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

package config

import support.GuiceAppSpec

class AppConfigSpec extends GuiceAppSpec{
  "appConfig" should {
    "specify the correct notable event types" in {
      appConfig.notableEvents.keySet shouldBe Set(
        "ppt-subscription",
        "itsa-annual-adjustment",
        "itsa-crystallisation",
        "vat-registration",
        "interest-restriction-return",
        "vat-return-ui",
        "itsa-eops",
        "entry-declaration",
        "vat-return")
    }
  }
}