package com.josiahebhomenye.crypto.remote

import java.io.{InputStream, OutputStream}
import java.net.{ServerSocket, Socket}
import java.util.Scanner

import com.josiahebhomenye.crypto.Logger
import io.netty.buffer.ByteBuf
import io.netty.channel.{Channel, ChannelHandlerContext}
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.util.CharsetUtil

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.Random

/**
  * Created by jay on 26/09/2016.
  */
trait TestServer {

  val MaxPort = 1 << 16
  val MinPort = 10000
  val host = "localhost"
  val port = Random.nextInt(MaxPort - MinPort) + MinPort

  implicit val ec = ExecutionContext.global

  protected var remote: ServerSocket = null
  protected var connect: Future[Socket] = null
  protected var out: Future[OutputStream] = null
  protected var in: Future[InputStream] = null


  def start() = {
    remote = new ServerSocket(port)
    connect = Future{
      Logger.info("server online waiting on connection")
      remote.accept()
    }
    out = connect.map(_.getOutputStream )
    in = connect.map(_.getInputStream)
  }

  def stop() = remote.close()

  implicit val stringDecoder = (in: InputStream) => {
    new Scanner(in).nextLine()
  }


  def emptyHandler = (ch: Channel) => ch.pipeline().addLast(new MessageToByteEncoder[String]() {
    override def encode(ctx: ChannelHandlerContext, msg: String, out: ByteBuf): Unit = {
      out.writeCharSequence(s"$msg\n", CharsetUtil.UTF_8)
    }
  })

  def captureFromServer[T](implicit decoder: InputStream => T) = {
    Await.result(in.map(decoder), 5 seconds)
  }

  def replyFromServerWhen[T](reply: PartialFunction[T, Any])(implicit decoder: InputStream => T, encoder: Any => Array[Byte]): Unit  = {
    in.flatMap{ in =>
      val req = decoder(in)
      val resp = reply(req)
      out.map{ out =>
        val bytes = encoder(resp)
        out.write(bytes)
        out.flush()
      }
    }.onFailure{ case e => throw e}
  }

  def sendFromServer[T](t: T)(implicit encoder: T => Array[Byte]): Future[Unit] = out.map { o =>
    o.write(encoder(t))
    o.flush()
  }

  def doAtServer(f: (Future[InputStream], Future[OutputStream]) => Unit): Unit = f(in, out)
}
