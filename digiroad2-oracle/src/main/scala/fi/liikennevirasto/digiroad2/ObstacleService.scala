package fi.liikennevirasto.digiroad2

import fi.liikennevirasto.digiroad2.asset.AdministrativeClass
import fi.liikennevirasto.digiroad2.pointasset.oracle._
import org.joda.time.DateTime

case class IncomingObstacle(lon: Double, lat: Double, linkId: Long, obstacleType: Int) extends IncomingPointAsset

class ObstacleService(val vvhClient: VVHClient) extends PointAssetOperations {
  type IncomingAsset = IncomingObstacle
  type PersistedAsset = Obstacle

  override def typeId: Int = 220

  override def fetchPointAssets(queryFilter: String => String, roadLinks: Seq[VVHRoadlink]): Seq[Obstacle] = OracleObstacleDao.fetchByFilter(queryFilter)

  override def setFloating(persistedAsset: Obstacle, floating: Boolean) = {
    persistedAsset.copy(floating = floating)
  }

  override def create(asset: IncomingObstacle, username: String, geometry: Seq[Point], municipality: Int, administrativeClass: Option[AdministrativeClass] = None): Long = {
    val mValue = GeometryUtils.calculateLinearReferenceFromPoint(Point(asset.lon, asset.lat, 0), geometry)
    withDynTransaction {
      OracleObstacleDao.create(asset, mValue, username, municipality)
    }
  }

  override def update(id: Long, updatedAsset: IncomingObstacle, geometry: Seq[Point], municipality: Int, username: String): Long = {
    val mValue = GeometryUtils.calculateLinearReferenceFromPoint(Point(updatedAsset.lon, updatedAsset.lat, 0), geometry)
    withDynTransaction {
      OracleObstacleDao.update(id, updatedAsset, mValue, username, municipality)
    }
    id
  }

  def getFloatingObstacles(floating: Int, lastIdUpdate: Long, batchSize: Int): Seq[Obstacle] = {
    OracleObstacleDao.selectFloatings(floating, lastIdUpdate, batchSize)
  }

  def updateFloatingAsset(obstacleUpdated: Obstacle) = {
    OracleObstacleDao.updateFloatingAsset(obstacleUpdated)
  }

}


