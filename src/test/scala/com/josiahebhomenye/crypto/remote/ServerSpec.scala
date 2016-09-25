package com.josiahebhomenye.crypto.remote

import java.net.ServerSocket

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, Channel}
import io.netty.handler.codec.MessageToByteEncoder

import scala.concurrent.Await
import scala.util.Random
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by jay on 24/09/2016.
  */
class ServerSpec extends CryptoClientSpec with OneServerPerTest{

  "Server connection" should {

    "be successful" in {
      val server = new Server(host, port, emptyHandler)
      val channel = await(server.run)
      channel.isActive mustBe true
    }

    "be able to send messages to the server" in {
      val server = new Server(host, port, emptyHandler)
      val channel = await(server.run)

      val msg = "Hello there server"
      channel.writeAndFlush(msg)

      msg mustBeReceivedAtServer
    }
  }

}
