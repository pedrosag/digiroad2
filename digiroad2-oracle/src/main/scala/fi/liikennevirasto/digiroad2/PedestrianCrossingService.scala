package fi.liikennevirasto.digiroad2

import com.jolbox.bonecp.{BoneCPConfig, BoneCPDataSource}
import fi.liikennevirasto.digiroad2.asset.BoundingRectangle
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.pointasset.oracle.{OraclePedestrianCrossingDao, PersistedPedestrianCrossing}
import fi.liikennevirasto.digiroad2.user.User
import slick.driver.JdbcDriver.backend.Database.dynamicSession
import slick.jdbc.StaticQuery.interpolation

trait FloatingAsset {
  val id: Long
  val floating: Boolean
}

trait PersistedPointAsset {
  val id: Long
  val lon: Double
  val lat: Double
  val municipalityCode: Int
}

trait RoadLinkAssociatedPointAsset extends PersistedPointAsset {
  val mmlId: Long
  val mValue: Double
  val floating: Boolean
}

trait PointAssetOperations[A <: FloatingAsset, B <: RoadLinkAssociatedPointAsset] {
  def roadLinkService: RoadLinkService
  lazy val dataSource = {
    val cfg = new BoneCPConfig(OracleDatabase.loadProperties("/bonecp.properties"))
    new BoneCPDataSource(cfg)
  }
  def withDynTransaction[T](f: => T): T = OracleDatabase.withDynTransaction(f)
  def withDynSession[T](f: => T): T = OracleDatabase.withDynSession(f)
  def typeId: Int
  def fetchPointAssets(queryFilter: String => String): Seq[B]
  def persistedAssetToAsset(persistedAsset: B, floating: Boolean): A

  def getByBoundingBox(user: User, bounds: BoundingRectangle): Seq[A] = {
    case class AssetBeforeUpdate(asset: A, persistedFloating: Boolean)

    val roadLinks = roadLinkService.fetchVVHRoadlinks(bounds)
    withDynSession {
      val boundingBoxFilter = OracleDatabase.boundingBoxFilter(bounds, "a.geometry")
      val filter = s"where a.asset_type_id = $typeId and $boundingBoxFilter"
      val persistedAssets: Seq[B] = fetchPointAssets(withFilter(filter))

      val assetsBeforeUpdate: Seq[AssetBeforeUpdate] = persistedAssets.filter { persistedAsset =>
        user.isAuthorizedToRead(persistedAsset.municipalityCode)
      }.map { persistedAsset =>
        val floating = PointAssetOperations.isFloating(persistedAsset, roadLinks.find(_.mmlId == persistedAsset.mmlId).map(link => (link.municipalityCode, link.geometry)))
        AssetBeforeUpdate(persistedAssetToAsset(persistedAsset, floating), persistedAsset.floating)
      }

      assetsBeforeUpdate.foreach { asset =>
        if (asset.asset.floating != asset.persistedFloating) {
          updateFloating(asset.asset.id, asset.asset.floating)
        }
      }

      assetsBeforeUpdate.map(_.asset)
    }
  }

  protected def withFilter(filter: String)(query: String): String = {
    query + " " + filter
  }

  protected def updateFloating(id: Long, floating: Boolean) = sqlu"""update asset set floating = $floating where id = $id""".execute
}

case class PedestrianCrossing(id: Long, mmlId: Long, lon: Double, lat: Double, mValue: Double, floating: Boolean) extends FloatingAsset

class PedestrianCrossingService(roadLinkServiceImpl: RoadLinkService) extends PointAssetOperations[PedestrianCrossing, PersistedPedestrianCrossing] {
  override def roadLinkService: RoadLinkService = roadLinkServiceImpl
  override def typeId: Int = 200
  override def fetchPointAssets(queryFilter: String => String): Seq[PersistedPedestrianCrossing] = OraclePedestrianCrossingDao.fetchByFilter(queryFilter)
  override def persistedAssetToAsset(persistedAsset: PersistedPedestrianCrossing, floating: Boolean) = {
    PedestrianCrossing(persistedAsset.id, persistedAsset.mmlId,
      persistedAsset.lon, persistedAsset.lat, persistedAsset.mValue, floating)
  }
}

object PointAssetOperations {
  def isFloating(persistedAsset: RoadLinkAssociatedPointAsset, roadLink: Option[(Int, Seq[Point])]): Boolean = {
    val point = Point(persistedAsset.lon, persistedAsset.lat)
    roadLink match {
      case None => true
      case Some((municipalityCode, geometry)) => municipalityCode != persistedAsset.municipalityCode ||
        !coordinatesWithinThreshold(Some(point), GeometryUtils.calculatePointFromLinearReference(geometry, persistedAsset.mValue))
    }
  }

  private val FLOAT_THRESHOLD_IN_METERS = 3

  def coordinatesWithinThreshold(pt1: Option[Point], pt2: Option[Point]): Boolean = {
    (pt1, pt2) match {
      case (Some(point1), Some(point2)) => point1.distanceTo(point2) <= FLOAT_THRESHOLD_IN_METERS
      case _ => false
    }
  }
}