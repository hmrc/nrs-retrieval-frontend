/*
 * Copyright 2018 HM Revenue & Customs
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

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.{Configuration, Environment}
import actors.{ActorService, ActorServiceImpl, RetrievalActor}
import config.MicroserviceAudit
import javax.inject.Provider
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.AppName

class Module(val environment: Environment, val configuration: Configuration) extends AbstractModule with AkkaGuiceSupport {

  def configure() = {
    bind(classOf[ActorService]).to(classOf[ActorServiceImpl])
    bindActor[RetrievalActor]("retrieval-actor")
    bind(classOf[Audit]).to(classOf[MicroserviceAudit])
    bind(classOf[String]).annotatedWith(Names.named("appName")).toProvider(AppNameProvider)
  }

  private object AppNameProvider extends Provider[String] {
    def get(): String = AppName(configuration).appName
  }

}
