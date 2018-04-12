package config

import models.audit.DataEventAuditType
import org.scalatest.mockito.MockitoSugar
import support.fixtures.Infrastructure
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.test.UnitSpec

class AuditableSpec extends UnitSpec with MockitoSugar with Infrastructure {

  "sendDataEvent" should {
    "send an audit event" in {
      // verify interactions with the mock audit
      val mockDataEventAuditType = mock[DataEventAuditType]
      auditable.sendDataEvent(mockDataEventAuditType)
    }
  }

  private val appName = "TestAppName"
  val mockAudit: Audit = mock[Audit]
  private val auditable = new Auditable(appName, mockAudit)
}
