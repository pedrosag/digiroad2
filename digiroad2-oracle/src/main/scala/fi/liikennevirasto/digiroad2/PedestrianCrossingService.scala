package fi.liikennevirasto.digiroad2

import com.jolbox.bonecp.{BoneCPConfig, BoneCPDataSource}
import fi.liikennevirasto.digiroad2.asset._
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.pointasset.oracle.{PedestrianCrossingToBePersisted, OraclePedestrianCrossingDao, PedestrianCrossing}
import fi.liikennevirasto.digiroad2.user.User
import org.joda.time.DateTime
import slick.driver.JdbcDriver.backend.Database.dynamicSession
import slick.jdbc.StaticQuery
import slick.jdbc.StaticQuery.interpolation

case class IncomingPedestrianCrossing(lon: Double, lat: Double, linkId: Long) extends IncomingPointAsset

class PedestrianCrossingService(val vvhClient: VVHClient) extends PointAssetOperations {
  type IncomingAsset = IncomingPedestrianCrossing
  type PersistedAsset = PedestrianCrossing

  override def typeId: Int = 200

  override def fetchPointAssets(queryFilter: String => String, roadLinks: Seq[VVHRoadlink]): Seq[PedestrianCrossing] = OraclePedestrianCrossingDao.fetchByFilter(queryFilter)

  override def setFloating(persistedAsset: PedestrianCrossing, floating: Boolean) = {
    persistedAsset.copy(floating = floating)
  }

  override def create(asset: IncomingPedestrianCrossing, username: String, geometry: Seq[Point], municipality: Int, administrativeClass: Option[AdministrativeClass] = None): Long = {
    val mValue = GeometryUtils.calculateLinearReferenceFromPoint(Point(asset.lon, asset.lat, 0), geometry)
    withDynTransaction {
      OraclePedestrianCrossingDao.create(PedestrianCrossingToBePersisted(asset.linkId, asset.lon, asset.lat, mValue, municipality, username), username)
    }
  }

  override def update(id: Long, updatedAsset: IncomingAsset, geometry: Seq[Point], municipality: Int, username: String): Long = {
    val mValue = GeometryUtils.calculateLinearReferenceFromPoint(Point(updatedAsset.lon, updatedAsset.lat, 0), geometry)
    withDynTransaction {
      OraclePedestrianCrossingDao.update(id, PedestrianCrossingToBePersisted(updatedAsset.linkId, updatedAsset.lon, updatedAsset.lat, mValue, municipality, username))
    }
    id
  }
}

