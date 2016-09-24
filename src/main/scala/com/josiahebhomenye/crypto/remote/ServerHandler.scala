package com.josiahebhomenye.crypto.remote

import com.cryptoutility.protocol.Events.Event
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}

import scala.concurrent.Future

sealed trait ChannelEvent{
  def ctx: ChannelHandlerContext
}

case class ChannelActive(ctx: ChannelHandlerContext) extends ChannelEvent
case class ChannelInActive(ctx: ChannelHandlerContext) extends ChannelEvent

object ServerHandler{

  type ChannelListener = ChannelEvent => Future[Unit]

  type EventListener = Event => Future[Unit]
}
import ServerHandler._

class ServerHandler(listeners: Seq[ChannelListener], eventListeners: Seq[EventListener]) extends SimpleChannelInboundHandler[Event]{


  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    println("connected to server")
    listeners.foreach(f => f(ChannelActive(ctx)))
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    listeners.foreach(f => f(ChannelInActive(ctx))) // TODO shutdown or try to reconnect when we lose the connection
  }

  override def channelRead0(ctx: ChannelHandlerContext, event: Event): Unit = {
    eventListeners.foreach(f => f(event))
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
  }

}
