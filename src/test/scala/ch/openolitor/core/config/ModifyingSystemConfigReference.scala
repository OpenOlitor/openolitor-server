package ch.openolitor.core.config

import ch.openolitor.core.{ MandantConfiguration, SystemConfig, SystemConfigReference }
import com.tegonal.CFEnvConfigLoader.ConfigLoader

trait ModifyingSystemConfigReference extends SystemConfigReference {
  override lazy val config = modifyConfig()
  override lazy val sysConfig = SystemConfig(MandantConfiguration("", "", "", 0, 0, Map(), config), null, null)

  protected def modifyConfig() = ConfigLoader.loadConfig
}
