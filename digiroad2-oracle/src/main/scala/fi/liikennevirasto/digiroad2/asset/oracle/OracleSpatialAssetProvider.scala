package fi.liikennevirasto.digiroad2.asset.oracle

import fi.liikennevirasto.digiroad2.asset._
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.user.UserProvider
import fi.liikennevirasto.digiroad2.{DigiroadEventBus, EventBusMassTransitStop}
import org.slf4j.LoggerFactory

trait DatabaseTransaction {
  def withDynTransaction[T](f: => T): T
}
object DefaultDatabaseTransaction extends DatabaseTransaction {
  override def withDynTransaction[T](f: => T): T = OracleDatabase.withDynTransaction(f)
}

// FIXME:
// - move common asset functionality to asset service
class OracleSpatialAssetProvider(spatialAssetDao: OracleSpatialAssetDao, eventbus: DigiroadEventBus, userProvider: UserProvider, databaseTransaction: DatabaseTransaction = DefaultDatabaseTransaction) extends AssetProvider {
  val logger = LoggerFactory.getLogger(getClass)

  def getAssetById(assetId: Long): Option[AssetWithProperties] = {
    databaseTransaction.withDynTransaction {
      spatialAssetDao.getAssetById(assetId)
    }
  }

  def getEnumeratedPropertyValues(assetTypeId: Long): Seq[EnumeratedPropertyValue] = {
    AssetPropertyConfiguration.commonAssetPropertyEnumeratedValues ++
      databaseTransaction.withDynTransaction {
        spatialAssetDao.getEnumeratedPropertyValues(assetTypeId)
      }
  }

  def availableProperties(assetTypeId: Long): Seq[Property] = {
    (AssetPropertyConfiguration.commonAssetProperties.values.map(_.propertyDescriptor).toSeq ++ databaseTransaction.withDynTransaction {
      spatialAssetDao.availableProperties(assetTypeId)
    }).sortBy(_.propertyUiIndex)
  }

  def getMunicipalities: Seq[Int] = {
    OracleDatabase.withDynSession {
      spatialAssetDao.getMunicipalities
    }
  }

  def assetPropertyNames(language: String): Map[String, String] = {
    AssetPropertyConfiguration.assetPropertyNamesByLanguage(language) ++ databaseTransaction.withDynTransaction {
      spatialAssetDao.assetPropertyNames(language)
    }
  }
}
