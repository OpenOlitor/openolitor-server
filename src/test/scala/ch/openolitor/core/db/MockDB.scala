package ch.openolitor.core.db

import ch.openolitor.core.SystemConfigReference
import scalikejdbc.ConnectionPool

trait MockDBComponent extends SystemConfigReference with ConnectionPoolContextAware with AsyncConnectionPoolContextAware {
  def initializeConnectionPool(): Unit = {
    ConnectionPool.singleton(sysConfig.mandantConfiguration.config.getString("db.default.url"), "tegonal", "tegonal")
  }

  override implicit def connectionPoolContext = MandantDBs(sysConfig.mandantConfiguration).connectionPoolContext()

  override implicit def asyncConnectionPoolContext = AsyncMandantDBs(sysConfig.mandantConfiguration).connectionPoolContext()
}
