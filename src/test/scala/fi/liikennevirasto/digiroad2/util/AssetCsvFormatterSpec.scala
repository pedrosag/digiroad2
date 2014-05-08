package fi.liikennevirasto.digiroad2.util

import org.scalatest._
import fi.liikennevirasto.digiroad2.asset.{PropertyValue, AssetWithProperties}
import fi.liikennevirasto.digiroad2.asset.oracle.OracleSpatialAssetProvider
import fi.liikennevirasto.digiroad2.user.oracle.OracleUserProvider

class AssetCsvFormatterSpec extends FlatSpec with MustMatchers with BeforeAndAfter with BeforeAndAfterAll {
  val userProvider = new OracleUserProvider
  val provider = new OracleSpatialAssetProvider(userProvider)
  var assetsByMunicipality: Iterable[AssetWithProperties] = null
  before {
    assetsByMunicipality = provider.getAssetsByMunicipality(235)
  }

  it must "return correct csv entries from test data" in {
    val csvAll = AssetCsvFormatter.formatAssetsWithProperties(assetsByMunicipality)
    csvAll.size must be > 3
    val csv = csvAll.find(_.startsWith("5")).get

    val propertyValue = extractPropertyValue(assetsByMunicipality.find(_.externalId.get == 5).get, _: String)
    val created = parseCreated(propertyValue("lisatty_jarjestelmaan"))

    val validFrom = propertyValue("ensimmainen_voimassaolopaiva")
    val validTo = propertyValue("viimeinen_voimassaolopaiva")

    csv must equal("5;;;;;374792.096855508;6677566.77442972;;;210;Etelään;;1;1;1;0;;;;" + created + ";dr1conversion;" + validFrom + ";" + validTo + ";Liikennevirasto;235;Kauniainen;;")
  }

  val testasset = AssetWithProperties(1, None, 1, 2.1, 2.2, 1, bearing = Some(3), validityDirection = None, wgslon = 2.2, wgslat = 0.56)
  it must "recalculate bearings in validity direction" in {
    AssetCsvFormatter.addBearing(testasset, List())._2 must equal (List("3"))
    AssetCsvFormatter.addBearing(testasset.copy(validityDirection = Some(3)), List())._2 must equal (List("183"))
    AssetCsvFormatter.addBearing(testasset.copy(validityDirection = Some(2)), List())._2 must equal (List("3"))
    AssetCsvFormatter.addBearing(testasset.copy(validityDirection = Some(2), bearing = Some(195)), List())._2 must equal (List("195"))
    AssetCsvFormatter.addBearing(testasset.copy(validityDirection = Some(3), bearing = Some(195)), List())._2 must equal (List("15"))
  }

  it must "describe bearing correctly" in {
    AssetCsvFormatter.addBearingDescription(testasset.copy(bearing = Some(316)), List())._2 must equal (List("Pohjoiseen"))
    AssetCsvFormatter.addBearingDescription(testasset.copy(bearing = Some(45)), List())._2 must equal (List("Pohjoiseen"))
    AssetCsvFormatter.addBearingDescription(testasset.copy(bearing = Some(46)), List())._2 must equal (List("Itään"))
    AssetCsvFormatter.addBearingDescription(testasset.copy(bearing = Some(135)), List())._2 must equal (List("Itään"))
    AssetCsvFormatter.addBearingDescription(testasset.copy(bearing = Some(136)), List())._2 must equal (List("Etelään"))
    AssetCsvFormatter.addBearingDescription(testasset.copy(bearing = Some(225)), List())._2 must equal (List("Etelään"))
    AssetCsvFormatter.addBearingDescription(testasset.copy(bearing = Some(226)), List())._2 must equal (List("Länteen"))
    AssetCsvFormatter.addBearingDescription(testasset.copy(bearing = Some(315)), List())._2 must equal (List("Länteen"))
  }

  it must "filter out newlines from text fields" in {
    val sourceAsset = assetsByMunicipality.find(_.externalId.get == 1).get
    val propertyValue = extractPropertyValue(sourceAsset, _: String)
    val created = parseCreated(propertyValue("lisatty_jarjestelmaan"))
    val validFrom = propertyValue("ensimmainen_voimassaolopaiva")
    val validTo = propertyValue("viimeinen_voimassaolopaiva")

    val testProperties = sourceAsset.propertyData.map { property =>
      property.publicId match {
        case "yllapitajan_tunnus" => property.copy(values = List(PropertyValue(propertyValue = "id\n", propertyDisplayValue = Some("id\n"))))
        case "matkustajatunnus" => property.copy(values = List(PropertyValue(propertyValue = "matkustaja\ntunnus", propertyDisplayValue = Some("matkustaja\ntunnus"))))
        case "nimi_suomeksi" => property.copy(values = List(PropertyValue(propertyValue = "n\nimi\n suomeksi", propertyDisplayValue = Some("n\nimi\n suomeksi"))))
        case "nimi_ruotsiksi" => property.copy(values = List(PropertyValue(propertyValue = "\nnimi ruotsiksi\n", propertyDisplayValue = Some("\nnimi ruotsiksi\n"))))
        case _ => property
      }
    }
    val asset = sourceAsset.copy(propertyData = testProperties)
    val csv = AssetCsvFormatter.formatFromAssetWithPropertiesValluCsv(asset)
    csv must equal("1;id;matkustajatunnus;nimi suomeksi;nimi ruotsiksi;374635.608258218;6677267.45072414;;;80;Itään;;1;0;0;0;katos;;;"
      + created
      + ";dr1conversion;" + validFrom + ";" + validTo + ";Liikennevirasto;235;Kauniainen;;")
  }

  private def extractPropertyValue(asset: AssetWithProperties, propertyPublicId: String): String = {
    asset.propertyData.find(_.publicId == propertyPublicId).get.values.head.propertyDisplayValue.getOrElse("")
  }

  private def parseCreated(s: String): String = {
    s.split(" ").drop(1).mkString(" ")
  }
}
