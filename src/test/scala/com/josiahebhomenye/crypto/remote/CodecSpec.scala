package com.josiahebhomenye.crypto.remote

import java.security.KeyPairGenerator
import java.util.UUID

import com.cryptoutility.protocol.EventSerializer
import com.cryptoutility.protocol.Events.{Event, UserInfo, Initialized}
import com.josiahebhomenye.crypto.remote.codec.Codec
import io.netty.buffer.{Unpooled, ByteBuf}
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.scalatest.{OptionValues, MustMatchers, WordSpec, Spec}

/**
  * Created by jay on 24/09/2016.
  */
class CodecSpec extends CryptoClientSpec{

  def toEvent(frame: BinaryWebSocketFrame) = EventSerializer.deserialize(frame.content().array())


  val event = Initialized(true, UserInfo("James", "Carl", "james@example.com", publicKey, id))

  "Codec" should {
    "be able to encode an event" in {
      val codec = new Codec
      val channel = new EmbeddedChannel(codec)

      channel.writeOutbound(event) mustBe true

      val outBound = toEvent(channel.readOutbound())

      outBound mustBe event
    }

    "be able to decode an inbound message" in {
      val codec = new Codec
      val frame = new BinaryWebSocketFrame(Unpooled.copiedBuffer(EventSerializer.serialize(event)))
      val channel = new EmbeddedChannel(codec)

      channel.writeInbound(frame) mustBe true

      val inBound = channel.readInbound[Event]()

      inBound mustBe event

    }
  }

}
