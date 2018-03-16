package com.dv.slots
package bus

import java.net.InetSocketAddress
import java.util.concurrent.Executors

import io.netty.buffer.ByteBuf
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{
  ChannelFuture
, ChannelInitializer
, ChannelOption
, ChannelHandlerContext
, ChannelInboundHandlerAdapter
}
import io.netty.handler.codec.{
  ByteToMessageDecoder
, MessageToByteEncoder
}

import akka.actor.{
  Props
, Actor
, ActorRef
, ActorContext
, ActorSystem
, Kill
}


sealed trait PlayerAction {}
case class Spin(bet:Int) extends PlayerAction
case class Connect(p:Int, ctx:ChannelHandlerContext) extends PlayerAction
case class Disconnect(p:Int, ctx:ChannelHandlerContext) extends PlayerAction
case class EnterGame(p:Int, m:Int) extends PlayerAction
case class LeaveGame(p:Int) extends PlayerAction


sealed trait GameEvent {}
case class BonusGame(spins:Int) extends GameEvent
case class Result(bet:Int, value:Seq[Seq[String]]) extends GameEvent {
  override def toString = {
    value.transpose.map(_.mkString("\t\t")).mkString("\n","\n","\n")
  }
}
case class BonusResult(value:Seq[Seq[String]]) extends GameEvent {
  override def toString = {
    value.transpose.map(_.mkString("\t\t")).mkString("\n","\n","\n")
  }
}
case class AvailableMachines(names:List[String]) extends GameEvent


object Msg { val hdr = 0xfafbfcfd }
sealed trait Msg { def code:Int; def p:Int }
case class LoginMsg(p:Int) extends Msg { def code = 1 }
case class LogoutMsg(p:Int) extends Msg { def code = 2 }
case class SpinMsg(p:Int, bet:Int) extends Msg { def code = 3 }
case class EnterGameMsg(p:Int, g:Int) extends Msg { def code = 4 }
case class LeaveGameMsg(p:Int) extends Msg { def code = 5 }

// lightweight encoder
case class SlotsEncoder() extends AbstractSlotsEncoder {}
abstract class AbstractSlotsEncoder extends MessageToByteEncoder[Msg] {
  override def encode(ctx:ChannelHandlerContext, in:Msg, out:ByteBuf) = {
    // Msg -> total legnth: 17 bytes
    out.writeInt(Msg.hdr)   // protocol hdr
    out.writeByte(in.code)  // message  type
    out.writeInt(in.p)      // player   id
    in match {
      case EnterGameMsg(p, g) => out.writeInt(g)      // game type
      case SpinMsg(p, b)      => out.writeInt(b)      // bet amount
      case _                  => out.writeInt(0x00)   // empty value
    }
  }
}

// lightweight decoder
case class SlotsDecoder() extends AbstractSlotsDecoder {}
abstract class AbstractSlotsDecoder extends ByteToMessageDecoder {
  override def decode(ctx:ChannelHandlerContext, in:ByteBuf, out:java.util.List[AnyRef]) = {
    // 17 bytes -> Msg
    while (in.readableBytes >= 1) {
      val hdr = in.readInt
      if (hdr == Msg.hdr) {
        val code = in.readByte
        code match {
          case 1 => { out.add(LoginMsg(in.readInt)); in.readInt }
          case 2 => { out.add(LogoutMsg(in.readInt)); in.readInt }
          case 3 => { out.add(SpinMsg(in.readInt, in.readInt)) }
          case 4 => { out.add(EnterGameMsg(in.readInt, in.readInt)) }
          case 5 => { out.add(LeaveGameMsg(in.readInt)); in.readInt }
        }
      }
    }
  }
}

@io.netty.channel.ChannelHandler.Sharable
object SlotsServerHandler extends ChannelInboundHandlerAdapter {

  import Application.{system => s}

  override def channelRead(ctx: ChannelHandlerContext, msg: Any) = {
    msg match {
      case LoginMsg(p:Int) => {
        println(s"Server: got login message from client $p")
        s.actorSelection("/user/lobby") ! Connect(p, ctx)
        ctx.writeAndFlush(LoginMsg(p))
      }
      case LogoutMsg(p:Int) => {
        s.actorSelection(s"/user/lobby/$p") ! Kill
      }
      case SpinMsg(p:Int, bet:Int) => {
        s.actorSelection(s"/user/lobby/$p") ! Spin(bet)
      }
      case EnterGameMsg(p:Int, m:Int) => {
        s.actorSelection(s"/user/lobby/$p") ! EnterGame(p, m)
      }
      case LeaveGameMsg(p:Int) => {
        s.actorSelection(s"/user/lobby/$p") ! LeaveGame(p)
      }
    }
  }

}

case class SlotsClientHandler(p:Int) extends ChannelInboundHandlerAdapter {

  import Application.{system => s}

  override def channelRead(ctx: ChannelHandlerContext, msg: Any) = {
    msg match {
      case LoginMsg(p) => {
        s.actorSelection(s"/user/$p") ! Connect(p, ctx)
      }
      case EnterGameMsg(_, m) => {
        s.actorSelection(s"/user/$p") ! EnterGame(p, m)
      }
      case x => {
        println(s"Unknown message received: $x")
      }
    }
  }

  override def channelInactive(ctx: ChannelHandlerContext) = {
    s.actorSelection(s"/user/$p") ! Disconnect(p, ctx)
  }

}
