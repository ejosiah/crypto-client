package com.josiahebhomenye.crypto.remote

import java.util.{ List => JList }

import com.cryptoutility.protocol.{Events, EventSerializer}
import com.cryptoutility.protocol.Events._
import com.cryptoutility.protocol.EventSerializer._
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.{MessageToMessageCodec, MessageToMessageEncoder, MessageToMessageDecoder}
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame

import scala.collection.mutable.ArrayBuffer

/**
  * Created by jay on 21/09/2016.
  */
object codec {

  class EventDecoder extends MessageToMessageDecoder[BinaryWebSocketFrame] {

    private [remote] var buf = ArrayBuffer[Byte]()
    private [remote] var length = Option.empty[Int]

    override def decode(ctx: ChannelHandlerContext, msg: BinaryWebSocketFrame, out: JList[AnyRef]): Unit = {
      buf ++= Unpooled.copiedBuffer(msg.content()).array()
      if(buf.length < HeaderSize){
        return
      }
      if(length.isEmpty) length = Some(readInt(buf.slice(0, IntSize).toArray))
      if(length.exists(buf.length < _)){
        return
      }
      while(length.exists(buf.length >= _)){
        val content = buf.slice(0, length.get)
        val event = EventSerializer.deserialize(content.toArray)
        out.add(event)  // TODO sort events before adding them
        buf = buf.slice(content.length, buf.length)
        if(buf.length >= IntSize) {
          length = Some(readInt(buf.slice(0, IntSize).toArray))
        }else{
          length = None
        }

      }
      println()
    }
  }


  class EventEncoder extends MessageToMessageEncoder[Event] {

    override def encode(ctx: ChannelHandlerContext, event: Event, out: JList[AnyRef]): Unit = {
      val data = EventSerializer.serialize(event)
      println(data.length)
      val frame = new BinaryWebSocketFrame(Unpooled.copiedBuffer(data))
      out.add(frame)
    }
  }

  class Codec extends MessageToMessageCodec[BinaryWebSocketFrame, Event]{
    val encoder = new EventEncoder
    val decoder = new EventDecoder

    override def encode(ctx: ChannelHandlerContext, msg: Event, out: JList[AnyRef]): Unit = encoder.encode(ctx, msg, out)

    override def decode(ctx: ChannelHandlerContext, msg: BinaryWebSocketFrame, out: JList[AnyRef]): Unit = decoder.decode(ctx, msg, out)
  }

}
