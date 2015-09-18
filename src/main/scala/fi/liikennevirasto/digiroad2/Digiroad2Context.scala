package fi.liikennevirasto.digiroad2

import java.util.Properties

import akka.actor.{Actor, ActorSystem, Props}
import fi.liikennevirasto.digiroad2.asset.AssetProvider
import fi.liikennevirasto.digiroad2.asset.oracle.{DatabaseTransaction, DefaultDatabaseTransaction}
import fi.liikennevirasto.digiroad2.linearasset.SpeedLimitProvider
import fi.liikennevirasto.digiroad2.linearasset.SpeedLimitFiller.ChangeSet
import fi.liikennevirasto.digiroad2.linearasset.SpeedLimitFiller.UnknownLimit
import fi.liikennevirasto.digiroad2.municipality.MunicipalityProvider
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.user.UserProvider
import fi.liikennevirasto.digiroad2.vallu.ValluSender
import slick.driver.JdbcDriver.backend.Database

class ValluActor extends Actor {
  def receive = {
    case (massTransitStop: EventBusMassTransitStop) => ValluSender.postToVallu(massTransitStop)
    case _                                          => println("received unknown message")
  }
}

class SpeedLimitUpdater(speedLimitProvider: SpeedLimitProvider) extends Actor {
  def receive = {
    case x: ChangeSet => persistSpeedLimitChanges(x)
    case x: Set[Long] => speedLimitProvider.purgeUnknown(x)
    case x: Seq[UnknownLimit] => speedLimitProvider.persistUnknown(x)
    case _                      => println("speedLimitFiller: Received unknown message")
  }

  def persistSpeedLimitChanges(speedLimitChangeSet: ChangeSet) {
    speedLimitProvider.drop(speedLimitChangeSet.droppedAssetIds)
    speedLimitProvider.persistMValueAdjustments(speedLimitChangeSet.adjustedMValues)
    speedLimitProvider.persistSideCodeAdjustments(speedLimitChangeSet.adjustedSideCodes)
  }
}

class LinkPropertyUpdater(roadLinkService: RoadLinkService) extends Actor {
  def receive = {
    case w: RoadLinkChangeSet => roadLinkService.updateRoadLinkChanges(w)
    case _                    => println("linkPropertyUpdater: Received unknown message")
  }
}

object Digiroad2Context {
  val Digiroad2ServerOriginatedResponseHeader = "Digiroad2-Server-Originated-Response"
  lazy val properties: Properties = {
    val props = new Properties()
    props.load(getClass.getResourceAsStream("/digiroad2.properties"))
    props
  }

  val system = ActorSystem("Digiroad2")

  val vallu = system.actorOf(Props[ValluActor], name = "vallu")
  eventbus.subscribe(vallu, "asset:saved")

  val speedLimitUpdater = system.actorOf(Props(classOf[SpeedLimitUpdater], speedLimitProvider), name = "speedLimitUpdater")
  eventbus.subscribe(speedLimitUpdater, "speedLimits:update")
  eventbus.subscribe(speedLimitUpdater, "speedLimits:purgeUnknown")
  eventbus.subscribe(speedLimitUpdater, "speedLimits:persistUnknownLimits")

  val linkPropertyUpdater = system.actorOf(Props(classOf[LinkPropertyUpdater], roadLinkService), name = "linkPropertyUpdater")
  eventbus.subscribe(linkPropertyUpdater, "linkProperties:changed")

  lazy val authenticationTestModeEnabled: Boolean = {
    properties.getProperty("digiroad2.authenticationTestMode", "false").toBoolean
  }

  lazy val assetProvider: AssetProvider = {
    Class.forName(properties.getProperty("digiroad2.featureProvider"))
         .getDeclaredConstructor(classOf[DigiroadEventBus], classOf[UserProvider], classOf[DatabaseTransaction])
         .newInstance(eventbus, userProvider, DefaultDatabaseTransaction)
         .asInstanceOf[AssetProvider]
  }

  lazy val speedLimitProvider: SpeedLimitProvider = {
    Class.forName(properties.getProperty("digiroad2.speedLimitProvider"))
      .getDeclaredConstructor(classOf[DigiroadEventBus], classOf[RoadLinkService])
      .newInstance(eventbus, roadLinkService)
      .asInstanceOf[SpeedLimitProvider]
  }

  lazy val userProvider: UserProvider = {
    Class.forName(properties.getProperty("digiroad2.userProvider")).newInstance().asInstanceOf[UserProvider]
  }

  lazy val municipalityProvider: MunicipalityProvider = {
    Class.forName(properties.getProperty("digiroad2.municipalityProvider")).newInstance().asInstanceOf[MunicipalityProvider]
  }

  lazy val eventbus: DigiroadEventBus = {
    Class.forName(properties.getProperty("digiroad2.eventBus")).newInstance().asInstanceOf[DigiroadEventBus]
  }

  lazy val vvhClient: VVHClient = {
    new VVHClient(getProperty("digiroad2.VVHServiceHost"))
  }

  lazy val roadLinkService: RoadLinkService = {
    if (useVVHGeometry) {
      new VVHRoadLinkService(vvhClient, eventbus)
    } else RoadLinkService
  }

  lazy val assetService: AssetService = {
    new AssetService(roadLinkService)
  }

  lazy val massTransitStopService: MassTransitStopService = {
    class ProductionMassTransitStopService(val eventbus: DigiroadEventBus) extends MassTransitStopService {
      override def roadLinkService: RoadLinkService = Digiroad2Context.roadLinkService
      override def withDynTransaction[T](f: => T): T = OracleDatabase.withDynTransaction(f)
      override def withDynSession[T](f: => T): T = OracleDatabase.withDynSession(f)
    }
    new ProductionMassTransitStopService(eventbus)
  }

  lazy val linearAssetService: LinearAssetService = {
    new LinearAssetService(roadLinkService)
  }

  lazy val useVVHGeometry: Boolean = properties.getProperty("digiroad2.useVVHGeometry", "false").toBoolean

  val env = System.getProperty("env")
  def getProperty(name: String) = {
    val property = properties.getProperty(name)
    if(property != null)
      property
    else
      throw new RuntimeException(s"cannot find property $name for enviroment: $env")
  }
}