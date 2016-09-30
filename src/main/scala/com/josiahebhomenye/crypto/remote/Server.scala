package com.josiahebhomenye.crypto.remote

import java.io.File

import com.cryptoutility.protocol.Events.{Event, UserCreated, UserInfo}
import com.josiahebhomenye.crypto.NettyToScalaHelpers._
import com.josiahebhomenye.crypto._
import com.josiahebhomenye.crypto.remote.codec.Codec
import io.netty.bootstrap.Bootstrap
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ChannelPipeline, Channel, EventLoopGroup}
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpClientCodec}

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Success, Failure}
import scala.util.control.NonFatal


object Server{

  def apply(home: String, host: String, port: Int, handlerChain: Channel => ChannelPipeline)(implicit ec: ExecutionContext) = {
    new Server(host, port, handlerChain)
  }

  def webSocketHandlers(handler: ServerHandler, host: String, port: Int)(ch: Channel) = {
    val p = if(port == -1) "" else ":$port"
    ch.pipeline()
      .addLast(new HttpClientCodec())
      .addLast(new HttpObjectAggregator(65536))
      .addLast(WebSocketClientProtocolHandler(s"ws://$host$p/client", Int.MaxValue))
      .addLast(new Codec)
      .addLast(handler)
  }
}

class Server(host: String, port: Int, handler:  Channel => ChannelPipeline)(implicit ec: ExecutionContext) {
  val validPort = if(port < 0) 80 else port // TODO port should be in range
  var mayBeGroup: Option[EventLoopGroup] = None
  val _1MB = Math.pow(1024, 3).toInt

  def run: Future[Channel] = {
    val group = NioEventLoopGroup()
    val f: Future[Channel] =
      new Bootstrap()
        .group(group)
        .channel(classOf[NioSocketChannel])
        .remoteAddress(host, validPort)
        .handler(handler)
        .connect()

    f.onComplete{
      case Failure(NonFatal(e)) =>
        mayBeGroup = None
        Logger.info("unable to start client")
        e.printStackTrace()
      case Success(c) =>
        Logger.info(s"client successfully connected to $c")
        mayBeGroup = Some(group)
    }

    f
  }

  def isRunning = mayBeGroup.isDefined

  def stop(): Unit = mayBeGroup.foreach(_.shutdownGracefully().sync())
}
