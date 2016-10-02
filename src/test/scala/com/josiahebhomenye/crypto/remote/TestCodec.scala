package com.josiahebhomenye.crypto.remote

import java.util
import javax.net.ssl.SSLEngineResult.HandshakeStatus

import com.cryptoutility.protocol.EventSerializer
import com.cryptoutility.protocol.Events.Event
import com.josiahebhomenye.crypto.remote.codec.EventDecoder
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler.ClientHandshakeStateEvent

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

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    super.channelActive(ctx)
    ctx.fireUserEventTriggered(ClientHandshakeStateEvent.HANDSHAKE_COMPLETE)
  }
}
