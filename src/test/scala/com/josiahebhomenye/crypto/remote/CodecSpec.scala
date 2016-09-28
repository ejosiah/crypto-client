package com.josiahebhomenye.crypto.remote

import java.nio.file.{Paths, Files}
import java.security.KeyPairGenerator

import com.cryptoutility.protocol.EventSerializer
import com.cryptoutility.protocol.Events.{StreamPart, Event, Initialized, UserInfo}
import com.josiahebhomenye.crypto.remote.codec.Codec
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame

import scala.collection.mutable.ArrayBuffer

/**
  * Created by jay on 24/09/2016.
  */
class CodecSpec extends CryptoClientSpec{

  def toEvent(frame: BinaryWebSocketFrame) = EventSerializer.deserialize(frame.content().array())

  def  publicKey = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic

  val event = Initialized(isNew = true, UserInfo("James", "Carl", "james@example.com", publicKey, id))

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

    "be able to decode multiple inbound messages" in {
      val data = loadStreams
      val codec = new Codec
      val channel = new EmbeddedChannel(codec)
      val frame = new BinaryWebSocketFrame(Unpooled.copiedBuffer(data))

      channel.writeInbound(frame) mustBe true

      val events = (0 until 4).map(_ => channel.readInbound[Event]())
      events.foreach(_.isInstanceOf[StreamPart] mustBe true)
      events.foreach(_.isInstanceOf[StreamPart] mustBe true)
      codec.decoder.buf.length mustBe 0
      codec.decoder.length mustBe None
    }
  }

  "length field should be populated when decodes has 4 bytes left in the buffer" in {
    val data = loadStreams
    val initializedEvent = EventSerializer.serialize(event)
    val expectedLength = initializedEvent.length
    val remaining = initializedEvent.slice(0, EventSerializer.IntSize)
    val codec = new Codec
    val channel = new EmbeddedChannel(codec)
    val buf = Unpooled.buffer(data.length + remaining.length)
    buf.writeBytes(data)
    buf.writeBytes(remaining)
    val frame = new BinaryWebSocketFrame(buf)

    channel.writeInbound(frame) mustBe true

    val events = (0 until 4).map(_ => channel.readInbound[Event]())
    events.foreach(_.isInstanceOf[StreamPart] mustBe true)
    codec.decoder.buf.length mustBe 4
    codec.decoder.length mustBe Some(expectedLength)
  }

  "be able to complete after a partial load" in {
    val data = loadStreams
    val initializedEvent = EventSerializer.serialize(event)
    val partial = initializedEvent.slice(0, initializedEvent.length/2)
    val expectedLength = initializedEvent.length
    val codec = new Codec
    val channel = new EmbeddedChannel(codec)
    val buf = Unpooled.buffer(data.length + partial.length)
    buf.writeBytes(data)
    buf.writeBytes(partial)
    val frame = new BinaryWebSocketFrame(buf)

    channel.writeInbound(frame) mustBe true
    val events = (0 until 4).map(_ => channel.readInbound[Event]())
    events.foreach(_.isInstanceOf[StreamPart] mustBe true)
    codec.decoder.buf.length mustBe (expectedLength/2)
    codec.decoder.length mustBe Some(expectedLength)

    val theRest = initializedEvent.slice(partial.length, expectedLength)
    val frame2 = new BinaryWebSocketFrame(Unpooled.copiedBuffer(theRest))

    channel.writeInbound(frame2)

    val e = channel.readInbound[Event]()

    e mustBe event
  }

  def loadStreams = {
    val content = Files.readAllBytes(Paths.get("src/test/resources/poem.txt"))
    val size = content.length / 4
    val leftOver = content.length - (size * 3)
    val parts = Seq.fill[Int](3)(size) :+ (if (leftOver == 0) size else leftOver)
    var pos = 0
    val data = parts.zipWithIndex.map{ e =>
      val (n, i) = e
      val buf = new Array[Byte](n)
      Array.copy(content, pos, buf, 0, n)

      pos = pos + n
      EventSerializer.serialize(new StreamPart(i, buf))
    }.foldLeft(ArrayBuffer[Byte]())((buf, part) => buf ++= part ).toArray

    data
  }

}
