package support

import com.typesafe.config.ConfigFactory
import config.AppConfig
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig


trait Configs {

  def configuration: Configuration = Configuration(ConfigFactory.parseResources("application.conf"))

  def environment: Environment = Environment.simple()

  def servicesConfig = new ServicesConfig(configuration)

  implicit def appConfig: AppConfig = new AppConfig(configuration, environment, servicesConfig)
}