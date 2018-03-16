package com.dv.slots
package server
import bus._
import game._

import akka.actor.{
  Props,
  Actor,
  ActorContext
}

import io.netty.channel.ChannelHandlerContext

case class PlayerSession(p:Int, ctx:ChannelHandlerContext) extends Actor {

  override def receive = {

    case EnterGame(_, m) => {
      val machine = Machine.getInstance(m)
      context.actorOf(Props(GameSession(p, machine)), "game")
      ctx.writeAndFlush(EnterGameMsg(p, m))
    }

    case s @ Spin(bet) => {
      context.actorSelection("game") ! s
    }

    case r @ Result(bet, value) => {
      // we can withdraw player's money only upon result
      // write routine
      // ctx.write(value)
      println("Got result: " + r)
    }

    case b @ BonusResult(value) => {
      // transform value into something real
      // write routine
      // ctx.write(value)
      println("Got bonus result: " + b)
    }

  }

}

case class GameSession(p:Int, m:Machine) extends Actor {

  override def receive = normal

  def normal:Receive = {
    case Spin(bet)            => context.parent ! Result(bet, m.reels.map(_.spin))
    case BonusGame(spins:Int) => context.become(free(spins))
  }

  def free(spins:Int):Receive = {
    case Spin(bet)          => {
      context.parent ! BonusResult(m.reels.map(_.spin))
      if (spins > 1) {
        context.become(free(spins-1))
      } else {
        context.become(normal)
      }
    }
    case BonusGame(moreSpins:Int) => context.become(free(spins + moreSpins))
  }

}
