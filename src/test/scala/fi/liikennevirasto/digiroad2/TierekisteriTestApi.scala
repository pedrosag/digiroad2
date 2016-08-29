package fi.liikennevirasto.digiroad2

import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra._

class TierekisteriTestApi extends ScalatraServlet with JacksonJsonSupport {

  override protected implicit def jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  def mandatoryFields = Seq("valtakunnallinen_id", "livitunnus", "tienumero", "tieosanumero", "puoli", "ajorata", "etaisyys",
    "pikavuoro", "kayttajatunnus", "pysakin_tyyppi")

  def getMassTransitStop(): Map[String, Any] ={
    Map(
      "valtakunnallinen_id" -> 208914,
      "tienumero" -> 25823,
      "tieosanumero" -> 104,
      "ajorata" -> 0,
      "etaisyys" -> 150,
      "puoli" -> "oikea",
      "pysakin_tunnus" -> "681",
      "nimi_fi" -> "Raisionjoki",
      "pysakin_tyyppi" -> "paikallis",
      "pikavuoro" -> "ei",
      "alkupvm" -> "2016-01-01",
      "loppupvm" -> "2016-01-02",
      "lakkautuspvm" -> "2016-01-03",
      "livitunnus" -> "OTHJ208914",
      "nimi_se" -> "Reso å",
      "varusteet" -> getEquipments,
      "kayttajatunnus" -> "KX123456"
    )
  }

  def getEquipments():Map[String, Any] = {
    Map(
      "aikataulu" -> "ei_tietoa",
      "penkki" -> "ei_tietoa",
      "pyorateline" -> "ei_tietoa",
      "sahk_aikataulu" -> "ei_tietoa",
      "roskis" -> "ei_tietoa",
      "katos" -> "on",
      "mainoskatos" -> "ei",
      "saattomahd" -> "ei",
      "korotus" -> "on",
      "valaistus" -> "ei_tietoa"
    )
  }

  get("/pysakit") {
    List(
      getMassTransitStop
    )
  }

  get("/pysakit/:liviId"){
    val liviId = params("liviId")

    if(liviId != "OTHJ208914")
      halt(NotFound())

    getMassTransitStop
  }

  put("/pysakit/:liviId") {
    val liviId = params("liviId")

    if (liviId == "OTHJ20891499999999") {
      halt(BadRequest("Invalid 'mass transit stop' value for a field"))
    } else if (liviId == "OTHJ20891499Err") {
      halt(InternalServerError("Error in Tierekisteri System"))
    }

    val body = parsedBody.extract[Map[String, Any]]

    mandatoryFields.foreach { field =>
      body.get(field).getOrElse(halt(BadRequest("Malformed 'mass transit stop' parameter")))
    }

    if(liviId != "OTHJ208914")
      halt(NotFound())

    halt(NoContent())
  }

  post("/pysakit"){

    val body = parsedBody.extract[Map[String, Any]]

    mandatoryFields.foreach{ field  =>
      body.get(field).getOrElse(halt(BadRequest("Malformed 'mass transit stop' parameter")))
    }

    val stopId = body.get("valtakunnallinen_id").get.toString.toLong

    if(stopId == 208913)
      halt(Conflict())

    val liviId =  body.get("livitunnus").get.toString

    if(liviId.length > 10)
      halt(BadRequest())

    halt(NoContent())
  }

  delete("/pysakit/:liviId"){
    val liviId = params("liviId")

    if(liviId != "OTHJ208914")
      halt(NotFound())

    halt(NoContent())
  }
}
