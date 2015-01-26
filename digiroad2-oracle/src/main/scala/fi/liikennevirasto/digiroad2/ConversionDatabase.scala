package fi.liikennevirasto.digiroad2

import _root_.oracle.spatial.geometry.JGeometry
import com.jolbox.bonecp.{BoneCPConfig, BoneCPDataSource}
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase

import scala.slick.jdbc.{GetResult, PositionedResult}

object ConversionDatabase {
  implicit object GetPointSeq extends GetResult[Seq[Point]] {
    def apply(rs: PositionedResult) = toPoints(rs.nextBytes())
  }

  private def toPoints(bytes: Array[Byte]): Seq[Point] = {
    val geometry = JGeometry.load(bytes)
    if (geometry == null) Nil
    else {
      geometry.getOrdinatesArray.grouped(2).map { point ⇒
        Point(point(0), point(1))
      }.toList
    }
  }

  lazy val dataSource = {
    val cfg = new BoneCPConfig(OracleDatabase.loadProperties("/conversion.bonecp.properties"))
    new BoneCPDataSource(cfg)
  }
}