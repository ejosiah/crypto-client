package com.josiahebhomenye.crypto.remote

import com.josiahebhomenye.crypto.NettyToScalaHelpers._
import com.josiahebhomenye.crypto._
import com.josiahebhomenye.crypto.remote.codec.Codec
import io.netty.bootstrap.Bootstrap
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{Channel, EventLoopGroup}
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpClientCodec}
import sun.net.www.http.HttpClient

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Success, Failure}
import scala.util.control.NonFatal

/**
  * Created by jay on 20/09/2016.
  */
class Server(host: String, port: Int, handler:  ServerHandler)(implicit ec: ExecutionContext) {

  var mayBeGroup: Option[EventLoopGroup] = None
  val _1MB = Math.pow(1024, 3).toInt

  def run: Future[Channel] = {
    val group = NioEventLoopGroup()
    val f: Future[Channel] =
      new Bootstrap()
        .group(group)
        .channel(classOf[NioSocketChannel])
        .remoteAddress(host, port)
        .handler { ch: Channel =>
          ch.pipeline()
            .addLast(new HttpClientCodec())
            .addLast(new HttpObjectAggregator(65536))
            .addLast(WebSocketClientProtocolHandler(s"ws://$host:$port/client", Int.MaxValue))
            .addLast(new Codec)
            .addLast(handler)
        }.connect()

    f.onComplete{
      case Failure(NonFatal(e)) =>
        mayBeGroup = Some(group)
        println("unable to start client")
        e.printStackTrace()
      case Success(c) =>
        println(s"client successfully connected to $c")
        mayBeGroup = Some(group)
    }

    f
  }

  def isRunning = mayBeGroup.isDefined

  def stop(): Unit = mayBeGroup.foreach(_.shutdownGracefully().sync())
}
