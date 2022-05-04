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

package controllers

import support.UnitSpec

class StrideAuthSettingsSpec extends UnitSpec {
  private val settings = new StrideAuthSettings()
  
  "strideAuthEnabled" should {
    "be true" in {
      settings.strideAuthEnabled shouldBe true
    }
  }

  "strideRoles" should {
    """be "nrs digital investigator" and "nrs_digital_investigator" """ in {
      settings.strideRoles shouldBe Set("nrs digital investigator", "nrs_digital_investigator")
    }
  }
}
