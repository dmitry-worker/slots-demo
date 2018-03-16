package com.dv.slots

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

// common between server and clients
object Application {

  // setup actor system
  val system = ActorSystem()

  // this random is never used for actual spins
  val random = scala.util.Random

  def main(args:Array[String]) {
    def throwInfo = throw new Exception("Run what? -client or -server?")
    if (args.size != 1) {
      throwInfo
    } else {
      args(0) match {
        case "-server" => server.SlotsServer.runServer
        case "-client" => client.SlotsClient.runClient
      }
    }
  }

}
