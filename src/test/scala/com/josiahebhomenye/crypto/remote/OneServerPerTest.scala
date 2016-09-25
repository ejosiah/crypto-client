package com.josiahebhomenye.crypto.remote

import java.io.{OutputStream, InputStream, PrintStream, PrintWriter}
import java.net.{Socket, InetSocketAddress, SocketAddress, ServerSocket}
import java.util.Scanner

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, Channel}
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.util.CharsetUtil

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Random

/**
  * Created by jay on 24/09/2016.
  */
trait OneServerPerTest { self: CryptoClientSpec =>

  val MaxPort = 1 << 16
  val MinPort = 10000
  val host = "localhost"
  val port = Random.nextInt(MaxPort - MinPort) + MinPort

  implicit val ec = ExecutionContext.global

  var remote: ServerSocket = null
  var connect: Future[Socket] = null
  var out: Future[OutputStream] = null
  var in: Future[InputStream] = null

  implicit class AtServer[T](underlying: T){

    def mustBeReceivedAtServer(implicit decoder: InputStream => T)  = assertServerReceived(underlying)
  }

  implicit val stringDecoder = (in: InputStream) => {
    new Scanner(in).nextLine()
  }

  override  def beforeEach(): Unit = {
    remote = new ServerSocket(port)
    connect = Future{ remote.accept() }
    out = connect.map(_.getOutputStream )
    in = connect.map(_.getInputStream)
  }

  override  def afterEach(): Unit = {
    remote.close()
  }

  def assertServerReceived[T](msg: T)(implicit decoder: InputStream => T) = {
    val result = self.await(in.map(decoder))
    result mustBe msg
  }

  def emptyHandler = (ch: Channel) => ch.pipeline().addLast(new MessageToByteEncoder[String]() {
    override def encode(ctx: ChannelHandlerContext, msg: String, out: ByteBuf): Unit = {
      out.writeCharSequence(msg + "\n", CharsetUtil.UTF_8)
    }
  })
}
