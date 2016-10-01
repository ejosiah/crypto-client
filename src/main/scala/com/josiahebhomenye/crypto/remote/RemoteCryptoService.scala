package com.josiahebhomenye.crypto.remote


import java.io.{DataOutputStream, File, FileOutputStream}
import java.nio.file.Files
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.{Key, KeyFactory}
import java.util.concurrent.{TimeoutException, TimeUnit, CountDownLatch}
import java.util.concurrent.locks.ReentrantLock

import com.cryptoutility.protocol.Events._
import com.josiahebhomenye.crypto.Logger
import com.josiahebhomenye.crypto.NettyToScalaHelpers._
import com.josiahebhomenye.crypto.service.CryptoService
import com.typesafe.config.Config
import io.netty.channel.Channel

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal
import scala.concurrent.duration._

object RemoteCryptoService{
  var maybeKey = Option.empty[Key]

  def apply(unwrap: String => Key, decrypt: (String, Key) => String)(implicit ec: ExecutionContext, conf: Config) =
    new RemoteCryptoService(unwrap, decrypt)(ec, conf)

  def loadPrivateKey(implicit conf: Config) = {
    if(maybeKey.isEmpty){
      val path = new File(conf.getString("user.workDir"), conf.getString("user.key.private"))
      val raw = Files.readAllBytes(path.toPath)
      val keySpec = new PKCS8EncodedKeySpec(raw)
      maybeKey = Some(KeyFactory.getInstance("RSA").generatePrivate(keySpec))
    }
    maybeKey.get.asInstanceOf[RSAPrivateKey]
  }
}
// FIXME not thread safe
class RemoteCryptoService(unwrap: String => Key, decrypt: (String, Key) => String)(implicit ec: ExecutionContext, conf: Config) extends CryptoService {

  val algorithm = conf.getString("cipher.asymmetric.algorithm")
  val symmetricAlgo = conf.getString("cipher.symmetric.algorithm")
  val lock = new ReentrantLock()
  val connectionLatch = new CountDownLatch(1)
  val handShakeLatch = new CountDownLatch(1)
  var mayBeConnection = Option.empty[Channel]
  var maybePromise: Option[Any] = None
  var mayBeUser = Option.empty[UserInfo]
  val writer = new StreamWriter(unwrap, decrypt)

 def initialise(userInfo: UserInfo, isNew: Boolean): Future[UserCreated] = Future{
   handShakeLatch.await()
   if(mayBeConnection.isEmpty){
     Logger.info(s"${Thread.currentThread().getName}, connection not yet available, going to wait")
     if(!connectionLatch.await(10, TimeUnit.SECONDS)){
       val msg = "unable to acquire connection"
       Logger.info(s"${Thread.currentThread().getName}: $msg")
       throw new TimeoutException(msg)
     }
   }
   val connection = mayBeConnection.get

   val promise = Promise[UserCreated]
   maybePromise = Some(promise)
   connection.writeAndFlush(Initialized(isNew, userInfo)).onFailure{
     case NonFatal(e) => promise.failure(e)
   }
   promise.future
 }.flatMap(identity)

  def notify(event: ChannelEvent): Future[Unit] = Future {
    event match {
      case ChannelActive(ctx) =>
        mayBeConnection = Some(ctx.channel())
        Logger.info(s"connection ${mayBeConnection.get} acquired, resuming execution")
        connectionLatch.countDown()
      case ChannelInActive(_) =>
        Logger.info("disconnected from server")
        mayBeConnection = None
      case HandShakeCompleted =>
        handShakeLatch.countDown()
    }
  }

  def on(event: Event): Future[Unit] = Future{
    event match{
      case e: UserCreated =>
        mayBeUser = Some(e.user)
        maybePromise
          .map(_.asInstanceOf[Promise[UserCreated]])
          .foreach(_.success(e))
      case e: StreamEvent => writer.write(e)
    }
  }
}
