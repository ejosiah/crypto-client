package com.josiahebhomenye.crypto.remote

import com.cryptoutility.protocol.Events.{StreamEnded, Event}
import io.netty.channel.ChannelHandlerContext
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.{Future, Promise}

/**
  * Created by jay on 24/09/2016.
  */
class ServerHandlerSpec extends CryptoClientSpec with MockitoSugar with OneServerPerTest{


  val ctx = mock[ChannelHandlerContext]

  def listener[T](promise: Promise[T])(t: T) = Future.successful[Unit](promise.success(t))

  "ServerHandler" should{
    "forward channelReads to listeners" in {
      val promise = Promise[Event]()
      val l = listener(promise)(_)
      val handler = new ServerHandler(Seq(), Seq(l))
      val event = new StreamEnded(5)

      handler.channelRead0(ctx, event)

      val result = await(promise.future)

      result mustBe event
    }

    "forward channelActive events to listeners" in {
      val promise = Promise[ChannelEvent]()
      val l = listener(promise)(_)
      val handler = new ServerHandler(Seq(l), Seq())
      val event = new ChannelActive(ctx)

      handler.channelActive(ctx)

      val result = await(promise.future)
      result mustBe event
    }

    "forward ChannelInactive events to listeners" in{
      val promise = Promise[ChannelEvent]()
      val l = listener(promise)(_)
      val handler = new ServerHandler(Seq(l), Seq())
      val event = new ChannelActive(ctx)

      handler.channelActive(ctx)

      val result = await(promise.future)
      result mustBe event
    }
  }

}
