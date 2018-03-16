package com.dv.slots
package server
import bus._

import java.net.InetSocketAddress
import java.util.concurrent.Executors

// import com.trueaccord.scalapb.GeneratedMessage

import akka.actor.{
  Props
, Actor
, ActorContext
, ActorSystem
, Kill
}

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{
  ChannelFuture
, ChannelInitializer
, ChannelOption
, ChannelHandlerContext
, ChannelInboundHandlerAdapter
}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel

import io.netty.handler.codec.{ LengthFieldPrepender, LengthFieldBasedFrameDecoder }
import io.netty.handler.codec.protobuf.{ ProtobufDecoder, ProtobufEncoder }

import com.typesafe.config._


object SlotsServer {

  // setup netty w/nio
  val (bossGroup, workerGroup) = (new NioEventLoopGroup(5), new NioEventLoopGroup)

  val bootstrap = new ServerBootstrap()
  bootstrap.group(bossGroup, workerGroup)
    .channel(classOf[NioServerSocketChannel])
    .childHandler(new ChannelInitializer[SocketChannel] {
      override def initChannel(ch:SocketChannel) {
//         ch.pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
//      ch.pipeline.addLast("decoder", new ProtobufDecoder(SlotsMessage.newBuilder))
        ch.pipeline.addLast("decoder", SlotsDecoder())
//         ch.pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
//      ch.pipeline.addLast("encoder", new ProtobufEncoder())
        ch.pipeline.addLast("encoder", SlotsEncoder())
        ch.pipeline.addLast("handler", SlotsServerHandler)
      }
    })
    .option(ChannelOption.SO_BACKLOG, new java.lang.Integer(128))
    .option(ChannelOption.SO_KEEPALIVE, new java.lang.Boolean(true))


  def runServer = {
    try {
      // initialize player lobby
      val lobby = Application.system.actorOf(Props[Lobby], "lobby")

      // blocking listen
      val listenFuture = bootstrap.bind(19191).sync
      val closeFuture = listenFuture.channel.closeFuture.sync
    } catch {
      case e:Throwable => e.printStackTrace
    } finally {
      workerGroup.shutdownGracefully()
      bossGroup.shutdownGracefully()
    }
  }

}


case class Lobby() extends Actor {

  import Application.{system => s}

  override def receive = {
    case Connect(p, ctx)    => {
      // Maybe: send available machines back
      val ref = context.actorOf(Props(PlayerSession(p, ctx)), s"$p")
      // ref ! Machine.list
    }
  }
}
