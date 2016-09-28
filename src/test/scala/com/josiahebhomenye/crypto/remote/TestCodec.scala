package com.josiahebhomenye.crypto.remote

import java.util

import com.cryptoutility.protocol.EventSerializer
import com.cryptoutility.protocol.Events.Event
import com.josiahebhomenye.crypto.remote.codec.EventDecoder
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame

@Sharable
object TestCodec extends MessageToMessageCodec[ByteBuf, Event]{

  val decoder = new EventDecoder

  override def encode(ctx: ChannelHandlerContext, event: Event, out: util.List[AnyRef]): Unit = {
    val buf = Unpooled.buffer()
    buf.writeBytes(EventSerializer.serialize(event))
    out.add(buf)
  }

  override def decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: util.List[AnyRef]): Unit = {
    val buf = Unpooled.copiedBuffer(msg)
//    val event = EventSerializer.deserialize(buf.array())
//    out.add(event)
    val frame = new BinaryWebSocketFrame(buf)
    decoder.decode(ctx, frame, out)
  }
}
