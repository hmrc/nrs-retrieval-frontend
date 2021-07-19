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

/**
 * Base url, which depends on a `port` during tests has to be implemented in
 * lazy fashion.
 * Otherwise it will be impossible to run tests
 * based on GuiceOneServerPerSuite because `port` at which the test app is served
 * (which is needed to override `frontendBaseUrl`)
 * is initialized during application start and available after the application has started.
 *
 * On the other hand in order to start application you need to provide test-config which
 * needs a `port` in order to override `frontendBaseUrl`.
 *
 * If it was not implemented in lazy way it would lead to infinite loop
 */
abstract class FrontendBaseUrlProvider {

  def baseUrl(): String
}
