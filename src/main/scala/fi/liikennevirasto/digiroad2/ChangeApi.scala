package fi.liikennevirasto.digiroad2

import fi.liikennevirasto.digiroad2.Digiroad2Context._
import fi.liikennevirasto.digiroad2.asset.Asset._
import fi.liikennevirasto.digiroad2.asset.{SideCode, TrafficDirection}
import fi.liikennevirasto.digiroad2.linearasset.{PieceWiseLinearAsset, SpeedLimit}
import org.joda.time.DateTime
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport


class ChangeApi extends ScalatraServlet with JacksonJsonSupport with AuthenticationSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats

  before() {
    basicAuth
    contentType = formats("json")
  }

  get("/:assetType") {
    contentType = formats("json")
    val since = DateTime.parse(params("since"))
    val until = DateTime.parse(params("until"))
    params("assetType") match {
      case "speed_limits"                => speedLimitsToGeoJson(since, speedLimitService.getChanged(since, until))
      case "total_weight_limits"         => linearAssetsToGeoJson(since, linearAssetService.getChanged(30, since, until))
      case "trailer_truck_weight_limits" => linearAssetsToGeoJson(since, linearAssetService.getChanged(40, since, until))
      case "axle_weight_limits"          => linearAssetsToGeoJson(since, linearAssetService.getChanged(50, since, until))
      case "bogie_weight_limits"         => linearAssetsToGeoJson(since, linearAssetService.getChanged(60, since, until))
      case "height_limits"               => linearAssetsToGeoJson(since, linearAssetService.getChanged(70, since, until))
      case "length_limits"               => linearAssetsToGeoJson(since, linearAssetService.getChanged(80, since, until))
      case "width_limits"                => linearAssetsToGeoJson(since, linearAssetService.getChanged(90, since, until))
    }
  }

  private def speedLimitsToGeoJson(since: DateTime, speedLimits: Seq[ChangedSpeedLimit]) =
    Map(
      "type" -> "FeatureCollection",
      "features" ->
        speedLimits.map { case ChangedSpeedLimit(speedLimit, link) =>
          Map(
            "type" -> "Feature",
            "id" -> speedLimit.id,
            "geometry" -> Map(
              "type" -> "LineString",
              "coordinates" -> speedLimit.geometry.map(p => Seq(p.x, p.y, p.z))
            ),
            "properties" ->
              Map(
                "value" -> speedLimit.value.map(_.toJson),
                "link" -> Map(
                  "type" -> "Feature",
                  "id" -> link.linkId,
                  "geometry" -> Map(
                    "type" -> "LineString",
                    "coordinates" -> link.geometry.map(p => Seq(p.x, p.y, p.z))
                  ),
                  "properties" -> Map(
                    "functionalClass" -> link.functionalClass,
                    "type" -> link.linkType.value,
                    "length" -> link.length
                  )
                ),
                "sideCode" -> (link.trafficDirection match {
                  case TrafficDirection.AgainstDigitizing =>
                    SideCode.AgainstDigitizing.value
                  case TrafficDirection.TowardsDigitizing =>
                    SideCode.TowardsDigitizing.value
                  case _ =>
                    speedLimit.sideCode.value
                }),
                "startMeasure" -> speedLimit.startMeasure,
                "endMeasure" -> speedLimit.endMeasure,
                "createdBy" -> speedLimit.createdBy,
                "modifiedAt" -> speedLimit.modifiedDateTime.map(DateTimePropertyFormat.print(_)),
                "createdAt" -> speedLimit.createdDateTime.map(DateTimePropertyFormat.print(_)),
                "modifiedBy" -> speedLimit.modifiedBy,
                "changeType" -> extractChangeType(since, speedLimit.expired, speedLimit.createdDateTime)
              )
          )
        }
    )

  private def linearAssetsToGeoJson(since: DateTime, changedLinearAssets: Seq[ChangedLinearAsset]) =
    Map(
      "type" -> "FeatureCollection",
      "features" ->
        changedLinearAssets.map { case ChangedLinearAsset(linearAsset, link) =>
          Map(
            "type" -> "Feature",
            "id" -> linearAsset.id,
            "geometry" -> Map(
              "type" -> "LineString",
              "coordinates" -> linearAsset.geometry.map(p => Seq(p.x, p.y, p.z))
            ),
            "properties" ->
              Map(
                "value" -> linearAsset.value.map(_.toJson),
                "link" -> Map(
                  "type" -> "Feature",
                  "id" -> link.linkId,
                  "geometry" -> Map(
                    "type" -> "LineString",
                    "coordinates" -> link.geometry.map(p => Seq(p.x, p.y, p.z))
                  ),
                  "properties" -> Map(
                    "functionalClass" -> link.functionalClass,
                    "type" -> link.linkType.value,
                    "length" -> link.length
                  )
                ),
                "sideCode" -> (link.trafficDirection match {
                  case TrafficDirection.AgainstDigitizing =>
                    SideCode.AgainstDigitizing.value
                  case TrafficDirection.TowardsDigitizing =>
                    SideCode.TowardsDigitizing.value
                  case _ =>
                    linearAsset.sideCode.value
                }),
                "startMeasure" -> linearAsset.startMeasure,
                "endMeasure" -> linearAsset.endMeasure,
                "createdBy" -> linearAsset.createdBy,
                "modifiedAt" -> linearAsset.modifiedDateTime.map(DateTimePropertyFormat.print(_)),
                "createdAt" -> linearAsset.createdDateTime.map(DateTimePropertyFormat.print(_)),
                "modifiedBy" -> linearAsset.modifiedBy,
                "changeType" -> extractChangeType(since, linearAsset.expired, linearAsset.createdDateTime)
              )
          )
        }
    )

  private def extractChangeType(since: DateTime, expired: Boolean, createdDateTime: Option[DateTime]) = {
    if (expired) {
      "Remove"
    } else if (createdDateTime.exists(_.isAfter(since))) {
      "Add"
    } else {
      "Modify"
    }
  }
}
