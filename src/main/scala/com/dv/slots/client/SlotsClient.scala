package com.dv.slots
package client
import bus._

import java.net.InetSocketAddress
import java.util.concurrent.Executors

import akka.actor.{
  Props
, Actor
, ActorRef
, ActorContext
, ActorSystem
, Kill
}

import io.netty.bootstrap.Bootstrap
import io.netty.channel.{
  ChannelFuture
, ChannelInitializer
, ChannelOption
, ChannelHandlerContext
, ChannelInboundHandlerAdapter
}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel

import io.netty.handler.codec.{ LengthFieldPrepender, LengthFieldBasedFrameDecoder }
import io.netty.handler.codec.protobuf.{ ProtobufDecoder, ProtobufEncoder }

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import com.typesafe.config._

object SlotsClient {

  val workerGroup = new NioEventLoopGroup()
  val bootstrap = new Bootstrap()
  bootstrap.group(workerGroup)
  bootstrap.channel(classOf[NioSocketChannel])
  bootstrap.option(ChannelOption.SO_KEEPALIVE, new java.lang.Boolean(true))


  def runClient = {
    import Application.{ system => s }
    (0 until 200).map { p =>
      val clientInitializer = new ChannelInitializer[SocketChannel] {
        override def initChannel(ch: SocketChannel) {
          ch.pipeline.addLast("decoder", new SlotsDecoder)
          ch.pipeline.addLast("encoder", new SlotsEncoder)
          ch.pipeline.addLast("handler", new ChannelInboundHandlerAdapter {
            override def channelActive(ctx: ChannelHandlerContext) = {
              // hotswap replace handler after connect
              ctx.pipeline.replace("handler", "handler", SlotsClientHandler(p))
              val actor = s.actorOf(Props(client.SlotsClient(ctx, p)), s"$p")
            }
          })
        }
      }
      try {
        bootstrap.handler(clientInitializer)
        val future = bootstrap.connect("127.0.0.1", 19191) // .sync()
//         future.channel.closeFuture.sync()


      } finally {
        // free resources
      }
    }
  }

}


case class SlotsClient(ctx:ChannelHandlerContext, id:Int) extends Actor {

  import akka.actor.Cancellable

  def receive = offline

  val s = context.system.scheduler
  val delta = Application.random.nextInt(1000)
  def sched(f: => Unit) = { s.scheduleOnce((1000 + delta).millis)(f) }

  sched(ctx.writeAndFlush(LoginMsg(id)))

  def offline:Receive = {
    case Connect(_, _) => {
      println(s"I ($id) am entering lobby")
      sched(ctx.writeAndFlush(EnterGameMsg(id, 0)))
      context.become(online)
    }
  }

  def online:Receive = {
    case EnterGame(_, _) => {
      println(s"I ($id) am starting to play")
      sched(context.become(playing))
      startPlaying
    }
    case Disconnect(_, _) => {
      stopPlaying
      context.become(offline)
    }
  }

  def playing:Receive = {
    case Spin(bet) => {
      println(s"I ($id) spin with bet €$bet")
      sched(ctx.writeAndFlush( SpinMsg(id, bet) ))
    }
    case Disconnect(_, _) => {
      stopPlaying
      context.become(offline)
    }
  }




  ////////////////////////////
  // Private

  private var timer:Option[Cancellable] = None

  private def startPlaying:Unit = {
    val s = context.system.scheduler
    val random = Application.random
    def randomTime = 1 + random.nextInt(3)
    def randomBets = 5 + random.nextInt(9) * 5
    val cb = new Runnable {
      def run = {
        self ! Spin(randomBets)
        startPlaying
      }
    }
    timer = Some( s.scheduleOnce(randomTime.seconds, cb) )
  }

  private def stopPlaying:Unit = {
    timer.map { s => s.cancel }
    timer = None
  }

}
