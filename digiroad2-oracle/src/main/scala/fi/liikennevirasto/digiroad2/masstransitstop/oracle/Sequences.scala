package fi.liikennevirasto.digiroad2.masstransitstop.oracle

import fi.liikennevirasto.digiroad2.masstransitstop.oracle.Queries._
import slick.driver.JdbcDriver.backend.Database
import Database.dynamicSession

object Sequences {
  def nextPrimaryKeySeqValue = {
    nextPrimaryKeyId.as[Long].first
  }

  def nextLrmPositionPrimaryKeySeqValue = {
    nextLrmPositionPrimaryKeyId.as[Long].first
  }

  def nextViitePrimaryKeySeqValue = {
    nextViitePrimaryKeyId.as[Long].first
  }
}
