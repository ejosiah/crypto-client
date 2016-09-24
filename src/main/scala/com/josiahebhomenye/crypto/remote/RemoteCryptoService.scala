package com.josiahebhomenye.crypto.remote


import java.security.KeyPairGenerator

import com.cryptoutility.protocol.Events.{UserCreated, Event, Initialized}
import com.cryptoutility.protocol.Events.UserInfo
import com.josiahebhomenye.crypto.service.CryptoService
import io.netty.channel.{Channel, ChannelHandlerContext}
import com.josiahebhomenye.crypto.NettyToScalaHelpers._

import scala.concurrent.{Promise, ExecutionContext, Future}
import scala.util.control.NonFatal

class RemoteCryptoService(path: String)(implicit ec: ExecutionContext) extends CryptoService {

  var mayBeConnection = Option.empty[Channel]
  var maybePromise: Option[Any] = None

 def initialise(userInfo: UserInfo): Future[UserCreated] = {
   require(mayBeConnection.isDefined, "No connection available")
   val connection = mayBeConnection.get

   val promise = Promise[UserCreated]
   maybePromise = Some(promise)
   connection.writeAndFlush(Initialized(userInfo.clientId.isEmpty, userInfo)).onFailure{
     case NonFatal(e) => promise.failure(e)
   }
   promise.future
 }

  def notify(event: ChannelEvent): Future[Unit] = Future {
    event match {
      case ChannelActive(ctx) => mayBeConnection = Some(ctx.channel())
      case ChannelInActive(_) => mayBeConnection = None
    }
  }

  def on(event: Event): Future[Unit] = Future{
    event match{
      case e: UserCreated =>
        maybePromise
          .map(_.asInstanceOf[Promise[UserCreated]])
          .foreach(_.success(e))
    }
  }
}
