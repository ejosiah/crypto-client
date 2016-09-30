package com.josiahebhomenye.crypto.remote

import com.cryptoutility.protocol.Events.Event
import com.josiahebhomenye.crypto.Logger
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import com.typesafe.config.Config
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

sealed trait ChannelEvent{
  def ctx: ChannelHandlerContext
}

case class ChannelActive(ctx: ChannelHandlerContext) extends ChannelEvent
case class ChannelInActive(ctx: ChannelHandlerContext) extends ChannelEvent
case class ExceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) extends ChannelEvent

object ServerHandler{

  type ChannelListener = ChannelEvent => Future[Unit]

  type EventListener = Event => Future[Unit]
}
import ServerHandler._

class ServerHandler(listeners: Seq[ChannelListener], eventListeners: Seq[EventListener])(implicit ec: ExecutionContext, conf: Config) extends SimpleChannelInboundHandler[Event]{


  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    Logger.info("connected to server")
    listeners.foreach(f => f(ChannelActive(ctx)))
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    listeners.foreach{ f =>
      // TODO shutdown or try to reconnect when we lose the connection
      f(ChannelInActive(ctx)).onFailure{ case NonFatal(e) => ctx.fireExceptionCaught(e) }
    }
  }

  override def channelRead0(ctx: ChannelHandlerContext, event: Event): Unit = {
    eventListeners.foreach{ f =>
      f(event).onFailure{ case NonFatal(e) => ctx.fireExceptionCaught(e) }
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    listeners.foreach(f => f(ExceptionCaught(ctx, cause))) // TODO use a differnt handler for errors
    throw cause
  }

}
