package com.dv.slots
package server.game
import bus._

import com.typesafe.config.{
  Config
, ConfigObject
, ConfigFactory
}

import akka.actor.{
  Props,
  Actor,
  ActorContext
}


case class Reel(height:Int, weights:List[(String, Double)]) {

  // In between of util.Random() weakness and NativePRNG performance.
  val rand = java.security.SecureRandom.getInstance("SHA1PRNG")

  lazy val total = this.weights.foldLeft(0D)( (res, t) => res + t._2)
  lazy val yAxis = (0 until this.height).toList

  def spin:List[String] = {

    def spin0(remainder:Double, ws:List[(String, Double)]):String = {
      ws match {
        case h :: tail => {
          val rem = remainder - h._2
          if (rem > 0) spin0(rem, tail)
          else h._1
        }
        case Nil => {
          throw new RuntimeException("Cannot happen")
        }
      }
    }

    yAxis.map { i => spin0(rand.nextDouble() * this.total, this.weights) }

  }

}

case class Machine(reels:List[Reel])

object Machine {

  import scala.collection.JavaConversions._
  implicit def toConfig(conf:ConfigObject):Config = { conf.toConfig }

  def getInstance(id:Int) = {
    // every time we re-read configuration
    val config = ConfigFactory.load("machines.conf")
    val machines = config.getObject("machines")
    val obj = machines.getObject("first") // dirty hack!

    val reelHeight = obj.getInt("height")
    val reels  = obj.getObjectList("reels").map { reelConfig =>
      val symbolSeq = reelConfig.keySet.map(name => {
        name -> reelConfig.getDouble(name)
      })
      Reel(reelHeight, symbolSeq.toList)
    }
    Machine(reels.toList)
  }

  def list = {
    val config = ConfigFactory.defaultApplication
    val machines = config.getObject("machines")
    AvailableMachines(machines.keySet.toList)
  }

    val part = total % hit
    val src = (0 until (total - hit)).map(_ => 0)
    val htt = (0 until hit).map(_ => 1)
    val empty = Array.fill(hit)(ArrayBuffer[Int]())
    (htt ++ src).foldLeft(empty -> 0) {
      case ((res, index), el) =>
      res(
    }
  }


}
