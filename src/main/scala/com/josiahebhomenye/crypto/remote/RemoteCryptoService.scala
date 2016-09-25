package com.josiahebhomenye.crypto.remote


import com.cryptoutility.protocol.Events.{Event, Initialized, UserCreated, UserInfo}
import com.josiahebhomenye.crypto.NettyToScalaHelpers._
import com.josiahebhomenye.crypto.service.CryptoService
import com.typesafe.config.Config
import io.netty.channel.Channel

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

class RemoteCryptoService(path: String)(implicit ec: ExecutionContext, conf: Config) extends CryptoService {

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
