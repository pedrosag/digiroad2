package fi.liikennevirasto.digiroad2.util

import java.net.URLEncoder
import java.util
import java.util.Properties

import fi.liikennevirasto.digiroad2.{Point, Vector3d}
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
/**
  * A road consists of 1-2 tracks (fi: "ajorata"). 2 tracks are separated by a fence or grass for example.
  * Left and Right are relative to the advancing direction (direction of growing m values)
  */
sealed trait Track {
  def value: Int
}
object Track {
  val values = Set(Combined, RightSide, LeftSide, Unknown)

  def apply(intValue: Int): Track = {
    values.find(_.value == intValue).getOrElse(Unknown)
  }

  case object Combined extends Track { def value = 0 }
  case object RightSide extends Track { def value = 1 }
  case object LeftSide extends Track { def value = 2 }
  case object Unknown extends Track { def value = 99 }
}

/**
  * Road Side (fi: "puoli") tells the relation of an object and the track. For example, the side
  * where the bus stop is.
  */
sealed trait RoadSide {
  def value: Int
}
object RoadSide {
  val values = Set(Right, Left, Between, End, Middle, Across, Unknown)

  def apply(intValue: Int): RoadSide = {
    values.find(_.value == intValue).getOrElse(Unknown)
  }

  case object Right extends RoadSide { def value = 1 }
  case object Left extends RoadSide { def value = 2 }
  case object Between extends RoadSide { def value = 3 }
  case object End extends RoadSide { def value = 7 }
  case object Middle extends RoadSide { def value = 8 }
  case object Across extends RoadSide { def value = 9 }
  case object Unknown extends RoadSide { def value = 0 }
}

/**
  * Lanes 1-* are on a track.
  */
sealed trait Lane {
  def value: Int
}
object Lane {
  val values = Set(Main, ByPassOnLeft, AdditionalOnRight, Other)

  def apply(intValue: Int): Lane = {
    values.find(_.value == intValue).getOrElse(Unknown)
  }

  case object Main extends Lane { def value = 1 }
  case object ByPassOnLeft extends Lane { def value = 2 }
  case object AdditionalOnRight extends Lane { def value = 3 }
  case object Other extends Lane { def value = 4 }
  case object Unknown extends Lane { def value = 99 }
}

case class VKMError(content: Map[String, Any], url: String)
case class RoadAddress(municipalityCode: Option[String], road: Int, roadPart: Int, track: Track, mValue: Int, deviation: Option[Double])
class VKMClientException(response: String) extends RuntimeException(response)

/**
  * A class to transform ETRS89-FI coordinates to road network addresses using Viitekehysmuunnin (VKM)
  * HTTP service
  */
class GeometryTransform {
  protected implicit val jsonFormats: Formats = DefaultFormats
  private def VkmCoordinates = "koordinaatit"
  private def VkmRoad = "tie"
  private def VkmRoadPart = "osa"
  private def VkmDistance = "etaisyys"
  private def VkmTrackCode = "ajorata"
  private def VkmSearchRadius = "sade"
  private def VkmQueryIdentifier = "tunniste"
  private def VkmMunicipalityCode = "kuntanro"
  private def VkmQueryRoadAddresses = "tieosoitteet"
  private def VkmError = "virhe"
  private def VkmOffsetToGivenCoordinates = "valimatka"
  private def VkmReturnErrorMessage = "virheteksti"
  private def VkmReturnCode = "palautusarvo"
  private def VkmRoadNumberInterval = "tievali"
  private def VkmReturnCodeOk = 1
  private def VkmReturnCodeError = 0
  // see page 16: http://www.liikennevirasto.fi/documents/20473/143621/tieosoitej%C3%A4rjestelm%C3%A4.pdf/
  private def NonPedestrianRoadNumbers = "1-62999"
  private def AllRoadNumbers = "1-99999"

  private def vkmUrl = {
    val properties = new Properties()
    properties.load(getClass.getResourceAsStream("/digiroad2.properties"))
    properties.getProperty("digiroad2.VKMUrl") + "/vkm/tieosoite?"
  }

  private def vkmPostUrl = {
    val properties = new Properties()
    properties.load(getClass.getResourceAsStream("/digiroad2.properties"))
    properties.getProperty("digiroad2.VKMUrl") + "/vkm/muunnos"
  }

  def urlParams(paramMap: Map[String, Option[Any]]) = {
    paramMap.filter(entry => entry._2.nonEmpty).map(entry => URLEncoder.encode(entry._1, "UTF-8")
       + "=" + URLEncoder.encode(entry._2.get.toString, "UTF-8")).mkString("&")
  }

  def jsonParams(paramMaps: List[Map[String, Option[Any]]]) = {
    Serialization.write(Map(VkmCoordinates -> paramMaps))
  }

  private def request(url: String): Either[Map[String, Any], VKMError] = {
    val request = new HttpGet(url)
    val client = HttpClientBuilder.create().build()
    val response = client.execute(request)
    try {
      if (response.getStatusLine.getStatusCode >= 400)
        return Right(VKMError(Map("error" -> "Request returned HTTP Error %d".format(response.getStatusLine.getStatusCode)), url))
      val content: Map[String, Any] = parse(StreamInput(response.getEntity.getContent)).values.asInstanceOf[Map[String, Any]]
      Left(content)
    } catch {
      case e: Exception => Right(VKMError(Map("error" -> e.getMessage), url))
    } finally {
      response.close()
    }
  }

  private def createFormPost(json: String) = {
    val urlParameters = new util.ArrayList[NameValuePair]()
    urlParameters.add(new BasicNameValuePair("in", "koordinaatti"))
    urlParameters.add(new BasicNameValuePair("out", "tieosoite"))
    urlParameters.add(new BasicNameValuePair("callback", ""))
    urlParameters.add(new BasicNameValuePair("tilannepvm", ""))
    urlParameters.add(new BasicNameValuePair("kohdepvm", ""))
    urlParameters.add(new BasicNameValuePair("alueetpois", ""))
    urlParameters.add(new BasicNameValuePair("et_erotin", ""))
    urlParameters.add(new BasicNameValuePair("json", json))
    new UrlEncodedFormEntity(urlParameters)
  }

  private def post(url: String, json: String): Either[List[Map[String, Any]], VKMError] = {
    val request = new HttpPost(url)
    request.setEntity(createFormPost(json))
    val client = HttpClientBuilder.create().build()
    val response = client.execute(request)
    try {
      val content: Map[String, Any] = parse(StreamInput(response.getEntity.getContent)).values.asInstanceOf[Map[String, Any]]
      content.get(VkmQueryRoadAddresses).map(_.asInstanceOf[List[Map[String, Any]]])
        .map(Left(_)).getOrElse(Right(VKMError(content, url)))
    } finally {
      response.close()
    }
  }

  private def roadNumberInterval(pedestrianIncluded: Option[Boolean], road: Option[Int]) = {
    if (road.nonEmpty)
      road.map(_.toString)
    else if (pedestrianIncluded.getOrElse(false))
      Option(AllRoadNumbers)
    else
      Option(NonPedestrianRoadNumbers)
  }

  def coordToAddress(coord: Point, road: Option[Int] = None, roadPart: Option[Int] = None,
                     distance: Option[Int] = None, track: Option[Track] = None, searchDistance: Option[Double] = None,
                     includePedestrian: Option[Boolean] = Option(false)) = {
    val params = Map(VkmRoad -> road, VkmRoadPart -> roadPart, VkmDistance -> distance, VkmTrackCode -> track.map(_.value),
    "x" -> Option(coord.x), "y" -> Option(coord.y), VkmSearchRadius -> searchDistance,
      VkmRoadNumberInterval -> roadNumberInterval(includePedestrian, road))
    request(vkmUrl + urlParams(params)) match {
      case Left(address) => mapFields(address)
      case Right(error) => throw new VKMClientException(error.toString)
    }
  }

  def coordsToAddresses(coords: Seq[Point], road: Option[Int] = None, roadPart: Option[Int] = None,
                        distance: Option[Int] = None, track: Option[Track] = None, searchDistance: Option[Double] = None,
                        includePedestrian: Option[Boolean] = Option(false)) = {
    val indexedCoords = coords.zipWithIndex
    val params = indexedCoords.map { case (coord, index) => Map(VkmRoad -> road, VkmRoadPart -> roadPart, VkmDistance -> distance, VkmTrackCode -> track.map(_.value),
      "x" -> Option(coord.x), "y" -> Option(coord.y), VkmSearchRadius -> searchDistance, VkmQueryIdentifier -> Option(index),
      VkmRoadNumberInterval -> roadNumberInterval(includePedestrian, road))
    }
    post(vkmPostUrl, jsonParams(params.toList)) match {
      case Left(addresses) => extractRoadAddresses(addresses)
      case Right(error) => throw new VKMClientException(error.toString)
    }
  }

  /**
    * Resolve side code as well as road address
    *
    * @param coord Coordinates of location
    * @param heading Geographical heading in degrees (North = 0, West = 90, ...)
    * @param road Road we want to find (optional)
    * @param roadPart Road part we want to find (optional)
    */
  def resolveAddressAndLocation(coord: Point, heading: Int, road: Option[Int] = None,
                                roadPart: Option[Int] = None,
                                includePedestrian: Option[Boolean] = Option(false)): (RoadAddress, RoadSide) = {
    if (road.isEmpty || roadPart.isEmpty) {
      val roadAddress = coordToAddress(coord, road, roadPart, includePedestrian = includePedestrian)
      resolveAddressAndLocation(coord, heading, Option(roadAddress.road), Option(roadAddress.roadPart))
    } else {
      val rad = (90-heading)*Math.PI/180.0
      val stepVector = Vector3d(3*Math.cos(rad), 3*Math.sin(rad), 0.0)
      val behind = coord - stepVector
      val front = coord + stepVector
      val addresses = coordsToAddresses(Seq(behind, coord, front), road, roadPart, includePedestrian = includePedestrian)
      val mValues = addresses.map(ra => ra.mValue)
      val (first, second, third) = (mValues(0), mValues(1), mValues(2))
      if (first <= second && second <= third && first != third) {
        (addresses(1), RoadSide.Right)
      } else if (first >= second && second >= third && first != third) {
        (addresses(1), RoadSide.Left)
      } else {
        (addresses(1), RoadSide.Unknown)
      }
    }

  }

  private def extractRoadAddresses(data: List[Map[String, Any]]) = {
    data.sortBy(_.getOrElse(VkmQueryIdentifier, Int.MaxValue).toString.toInt).map(mapFields)
  }

  private def mapFields(data: Map[String, Any]) = {
    val returnCode = data.getOrElse(VkmReturnCode, VkmReturnCodeOk)
    val error = data.get(VkmError)
    if (returnCode.toString.toInt == VkmReturnCodeError || error.nonEmpty)
      throw new VKMClientException("VKM error: %s".format(data.getOrElse(VkmReturnErrorMessage, error.getOrElse(""))))
    val municipalityCode = data.get(VkmMunicipalityCode)
    val road = validateAndConvertToInt(VkmRoad, data)
    val roadPart = validateAndConvertToInt(VkmRoadPart, data)
    val track = validateAndConvertToInt(VkmTrackCode, data)
    val mValue = validateAndConvertToInt(VkmDistance, data)
    val deviation = convertToDouble(data.get(VkmOffsetToGivenCoordinates))
    if (Track.apply(track).eq(Track.Unknown)) {
      throw new VKMClientException("Invalid value for Track (%s): %d".format(VkmTrackCode, track))
    }
    RoadAddress(municipalityCode.map(_.toString), road, roadPart, Track.apply(track), mValue, deviation)
  }

  private def validateAndConvertToInt(fieldName: String, map: Map[String, Any]) = {
    def value = map.get(fieldName)
    if (value.isEmpty) {
      throw new VKMClientException(
        "Missing mandatory field in response: %s".format(
          fieldName))
    }
    try {
      value.get.toString.toInt
    } catch {
      case e: NumberFormatException =>
        throw new VKMClientException("Invalid value in response: %s, Int expected, got '%s'".format(fieldName, value.get))
    }
  }


  private def convertToDouble(value: Option[Any]): Option[Double] = {
    value.map {
      case x: Object =>
        try {
          x.toString.toDouble
        } catch {
          case e: NumberFormatException =>
            throw new VKMClientException("Invalid value in response: Double expected, got '%s'".format(x))
        }
      case _ => throw new VKMClientException("Invalid value in response: Double expected, got '%s'".format(value.get))
    }
  }
}
