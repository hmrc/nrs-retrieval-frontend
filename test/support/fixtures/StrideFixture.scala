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

package support.fixtures

import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{GGCredId, LegacyCredentials, Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait StrideFixture extends MockitoSugar {
  val authConnector: AuthConnector = mock[AuthConnector]
  val role_name = "foo Role"
  val new_role_name = "foo_Role"
  val authResultOk: Future[~[LegacyCredentials, Enrolments]] =
    Future.successful(new ~(GGCredId("cred-1234"), Enrolments(Set(Enrolment(role_name), Enrolment(new_role_name)))))

  def authConnOk(authResult: Future[_]): AuthConnector = new AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
      authResult.map(_.asInstanceOf[A])
    }
  }
}

