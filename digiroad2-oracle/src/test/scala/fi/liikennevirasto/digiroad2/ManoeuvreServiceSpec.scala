package fi.liikennevirasto.digiroad2

import fi.liikennevirasto.digiroad2.asset._
import fi.liikennevirasto.digiroad2.linearasset.ValidityPeriodDayOfWeek.Saturday
import fi.liikennevirasto.digiroad2.linearasset.{ValidityPeriod, RoadLink}
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import slick.driver.JdbcDriver.backend.Database.dynamicSession
import slick.jdbc.StaticQuery.interpolation
import slick.jdbc.{StaticQuery => Q}


class ManoeuvreServiceSpec extends FunSuite with Matchers with BeforeAndAfter {
  private def vvhRoadLink(linkId: Long, municipalityCode: Int, geometry: Seq[Point] = Seq(Point(0, 0), Point(10, 0))) = {
    RoadLink(linkId, geometry, 10.0, Municipality, 5, TrafficDirection.UnknownDirection, SingleCarriageway, None, None)
  }

  val mockRoadLinkService = MockitoSugar.mock[RoadLinkService]
  when(mockRoadLinkService.getRoadLinksFromVVH(any[BoundingRectangle], any[Set[Int]]))
    .thenReturn(Seq(vvhRoadLink(1611419, 235), vvhRoadLink(1611412, 235), vvhRoadLink(1611410, 235)) )
  when(mockRoadLinkService.getRoadLinksFromVVH(municipality = 235))
    .thenReturn(Seq(vvhRoadLink(123, 235), vvhRoadLink(125, 235), vvhRoadLink(124, 235), vvhRoadLink(233, 235), vvhRoadLink(234, 235,  Seq(Point(15, 0), Point(20, 0)))))
  when(mockRoadLinkService.getRoadLinksFromVVH(any[Set[Long]]))
    .thenReturn(Seq(vvhRoadLink(1611420, 235), vvhRoadLink(1611411, 235)))

  val manoeuvreService = new ManoeuvreService(mockRoadLinkService)

  after {
    OracleDatabase.withDynTransaction {
      sqlu"""delete from manoeuvre_element where manoeuvre_id in (select id from manoeuvre where modified_by = 'unittest')""".execute
      sqlu"""delete from manoeuvre_validity_period where manoeuvre_id in (select id from manoeuvre where modified_by = 'unittest')""".execute
      sqlu"""delete from manoeuvre where modified_by = 'unittest'""".execute
    }
  }

  test("Get all manoeuvres partially or completely in bounding box") {
    val bounds = BoundingRectangle(Point(373880.25, 6677085), Point(374133, 6677382))
    val manoeuvres = manoeuvreService.getByBoundingBox(bounds, Set(235))
    manoeuvres.length should equal(3)
    val partiallyContainedManoeuvre = manoeuvres.find(_.id == 39561).get
    partiallyContainedManoeuvre.elements.find(_.elementType==ElementTypes.FirstElement).get.sourceLinkId should equal(1611419)
    partiallyContainedManoeuvre.elements.find(_.elementType==ElementTypes.FirstElement).get.destLinkId should equal(1611420)
    partiallyContainedManoeuvre.elements.find(_.elementType==ElementTypes.LastElement).get.sourceLinkId should equal(1611420)
    val completelyContainedManoeuvre = manoeuvres.find(_.id == 97666).get
    completelyContainedManoeuvre.elements.find(_.elementType==ElementTypes.FirstElement).get.sourceLinkId should equal(1611412)
    completelyContainedManoeuvre.elements.find(_.elementType==ElementTypes.LastElement).get.sourceLinkId should equal(1611410)
  }

  test("Filters out manoeuvres with non-adjacent source and destination links") {
    val manoeuvreId = manoeuvreService.createManoeuvre("unittest", NewManoeuvre(Set.empty, Nil, None, Seq(123, 125, 124)))
    val manoeuvreId2 = manoeuvreService.createManoeuvre("unittest", NewManoeuvre(Set.empty, Nil, None, Seq(233, 234)))

    val manoeuvres = manoeuvreService.getByMunicipality(235)

    manoeuvres.exists(_.id == manoeuvreId) should be(true)
    manoeuvres.exists(_.id == manoeuvreId2) should be(false)
  }

  def createManouvre: Manoeuvre = {
    val manoeuvreId = manoeuvreService.createManoeuvre("unittest", NewManoeuvre(Set(ValidityPeriod(0, 21, Saturday, 30, 45)), Nil, None, Seq(123, 124)))

    manoeuvreService.getByMunicipality(235)
      .find(_.id == manoeuvreId)
      .get
  }

  test("Create manoeuvre") {
    val manoeuvre: Manoeuvre = createManouvre

    manoeuvre.elements.head.sourceLinkId should equal(123)
    manoeuvre.elements.head.destLinkId should equal(124)
    manoeuvre.validityPeriods should equal(Set(ValidityPeriod(0, 21, Saturday, 30, 45)))
  }

  test("Delete manoeuvre") {
    val manoeuvre: Manoeuvre = createManouvre

    manoeuvreService.deleteManoeuvre("unittest", manoeuvre.id)

    val deletedManouver = manoeuvreService.getByMunicipality(235).find { m =>
      m.id == manoeuvre.id
    }
    deletedManouver should equal(None)
  }

  test("Get source road link id with manoeuvre id") {
    val manoeuvre: Manoeuvre = createManouvre

    val roadLinkId = manoeuvreService.getSourceRoadLinkIdById(manoeuvre.id)

    roadLinkId should equal(123)
  }

  test("Cleanup should remove extra elements") {
    val start = ManoeuvreElement(1, 1, 2, ElementTypes.FirstElement)
    val end = ManoeuvreElement(1, 4, 0, ElementTypes.LastElement)
    val intermediates = Seq(ManoeuvreElement(1, 2, 3, ElementTypes.IntermediateElement),
      ManoeuvreElement(1, 3, 8, ElementTypes.IntermediateElement),
      ManoeuvreElement(1, 3, 4, ElementTypes.IntermediateElement))
    val result = manoeuvreService.cleanChain(start, end, intermediates)
    result should have size 4
    result.exists(_.destLinkId == 8) should be (false)
  }

  test("Cleanup should remove loops") {
    val start = ManoeuvreElement(1, 1, 2, ElementTypes.FirstElement)
    val end = ManoeuvreElement(1, 5, 0, ElementTypes.LastElement)
    val intermediates = Seq(ManoeuvreElement(1, 2, 3, ElementTypes.IntermediateElement),
      ManoeuvreElement(1, 3, 4, ElementTypes.IntermediateElement),
      ManoeuvreElement(1, 4, 2, ElementTypes.IntermediateElement))
    val result = manoeuvreService.cleanChain(start, end, intermediates)
    result should have size 0
  }

  test("Cleanup should remove forks") {
    val start = ManoeuvreElement(1, 1, 2, ElementTypes.FirstElement)
    val end = ManoeuvreElement(1, 5, 0, ElementTypes.LastElement)
    val intermediates = Seq(ManoeuvreElement(1, 3, 5, ElementTypes.IntermediateElement),
      ManoeuvreElement(1, 2, 3, ElementTypes.IntermediateElement),
      ManoeuvreElement(1, 2, 5, ElementTypes.IntermediateElement))
    val result = manoeuvreService.cleanChain(start, end, intermediates)
    result.size > 2 should be (true)
  }

  test("Cleanup should not change working sequence") {
    val start = ManoeuvreElement(1, 1, 2, ElementTypes.FirstElement)
    val end = ManoeuvreElement(1, 5, 0, ElementTypes.LastElement)
    val intermediates = Seq(ManoeuvreElement(1, 2, 3, ElementTypes.IntermediateElement),
      ManoeuvreElement(1, 3, 4, ElementTypes.IntermediateElement),
      ManoeuvreElement(1, 4, 5, ElementTypes.IntermediateElement))
    val result = manoeuvreService.cleanChain(start, end, intermediates)
    result should have size 5
    result.filter(_.elementType == ElementTypes.IntermediateElement) should be (intermediates)
  }

  test("Cleanup should produce correct order") {
    val start = ManoeuvreElement(1, 1, 2, ElementTypes.FirstElement)
    val end = ManoeuvreElement(1, 5, 0, ElementTypes.LastElement)
    val intermediates = Seq(ManoeuvreElement(1, 2, 3, ElementTypes.IntermediateElement),
      ManoeuvreElement(1, 4, 5, ElementTypes.IntermediateElement),
      ManoeuvreElement(1, 3, 4, ElementTypes.IntermediateElement))
    val result = manoeuvreService.cleanChain(start, end, intermediates)
    result should have size 5
    result.filter(_.elementType == ElementTypes.IntermediateElement) shouldNot be (intermediates)
    val newIntermediates = result.filter(_.elementType == ElementTypes.IntermediateElement)
    newIntermediates.zip(newIntermediates.tail).forall(e => e._1.destLinkId == e._2.sourceLinkId) should be (true)
    newIntermediates.head.sourceLinkId should be (start.destLinkId)
    newIntermediates.last.destLinkId should be (end.sourceLinkId)
  }

  test("Validate a New Manoeuvre") {
    val roadLinksSeq : Seq[Long] = Seq(123, 124)
    val roadLinksSeq2 : Seq[Long] = Seq(123, 124, 125, 126)
    var result : Boolean = false

    val roadLinksNoValid = Seq(vvhRoadLink(123,235), vvhRoadLink(124,235,Seq(Point(25, 0), Point(30, 0))))
    val manoeuvreNoValid = NewManoeuvre(Set.empty, Nil, None, roadLinksSeq)
    result = manoeuvreService.isValid(manoeuvreNoValid, roadLinksNoValid)
    result should be (false)

    val roadLinksValid = Seq(vvhRoadLink(123,235), vvhRoadLink(124,235))
    val manoeuvreValid = NewManoeuvre(Set.empty, Nil, None, roadLinksSeq)
    result = manoeuvreService.isValid(manoeuvreValid, roadLinksValid)
    result should be (true)

    val roadLinksValid2 = Seq(vvhRoadLink(123,235), vvhRoadLink(124,235), vvhRoadLink(125,235), vvhRoadLink(126,235))
    val manoeuvreValid2 = NewManoeuvre(Set.empty, Nil, None, roadLinksSeq2)
    result = manoeuvreService.isValid(manoeuvreValid2, roadLinksValid2)
    result should be (true)

    val roadLinksNoValid2 = Seq(vvhRoadLink(123,235), vvhRoadLink(124,235,Seq(Point(25, 0), Point(30, 0))), vvhRoadLink(125,235), vvhRoadLink(126,235))
    val manoeuvreNoValid2 = NewManoeuvre(Set.empty, Nil, None, roadLinksSeq2)
    result = manoeuvreService.isValid(manoeuvreNoValid2, roadLinksNoValid2)
    result should be (false)
  }
}
