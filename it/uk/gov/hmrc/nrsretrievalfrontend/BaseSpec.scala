package uk.gov.hmrc.nrsretrievalfrontend

import org.scalatest.concurrent.PatienceConfiguration
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.reflect.ClassTag

trait BaseSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with PatienceConfiguration with BeforeAndAfterEach { this: Suite =>

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def injected[T](c: Class[T]): T = app.injector.instanceOf(c)
  def injected[T](implicit evidence: ClassTag[T]) = app.injector.instanceOf[T]


}
