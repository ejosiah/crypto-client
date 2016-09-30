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
  val streamEndLatch = new CountDownLatch(1)
  var mayBeConnection = Option.empty[Channel]
  var maybePromise: Option[Any] = None
  var mayBeOut = Option.empty[DataOutputStream]
  var mayBeUser = Option.empty[UserInfo]
  var mayBeStreamEnded = Option.empty[StreamEnded]
  var streamBuf = ArrayBuffer[StreamPart]()
  var currSeq = 0L // TODO use atomic here

 def initialise(userInfo: UserInfo, isNew: Boolean): Future[UserCreated] = {
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
 }

  def notify(event: ChannelEvent): Future[Unit] = Future {
    event match {
      case ChannelActive(ctx) =>
        mayBeConnection = Some(ctx.channel())
        Logger.info(s"connection ${mayBeConnection.get} acquired, resuming execution")
        connectionLatch.countDown()
      case ChannelInActive(_) => mayBeConnection = None
    }
  }

  def on(event: Event): Future[Unit] = Future{
    event match{
      case e: UserCreated =>
        mayBeUser = Some(e.user)
        maybePromise
          .map(_.asInstanceOf[Promise[UserCreated]])
          .foreach(_.success(e))
      case e: StreamStarted =>
        val secret = unwrap(e.secret)
        val filename = decrypt(e.filename, secret)
        Logger.info(s" streaming started for $filename")
        val path = conf.getString("user.workDir") + s"/$filename.krypt"
        mayBeOut = Some(new DataOutputStream(new FileOutputStream(path)))
        mayBeOut.foreach{ out =>
          out.writeUTF(e.secret)
          out.writeUTF(e.filename)
          out.writeUTF(e.contentType)
          out.writeUTF(e.from)
        }
        processBuffer()
        logState(e)
      case stream @ StreamPart(seqId, chunk) =>
        if(mayBeOut.isEmpty || seqId != currSeq){
          streamBuf = streamBuf += stream
          logState(stream)
        }else {
          write(stream)
          processBuffer()
          logState(stream)
        }
      case e @ StreamEnded(size) =>
          Logger.info(s"${Thread.currentThread()} size: $size")
          Logger.info(s"currSize: $currSeq")
          Logger.debug(s"buffer size: ${streamBuf.size}")
          processBuffer()
          Logger.debug(s"buffer size: ${streamBuf.size}")
        if(currSeq < size){
          val remaining = size - currSeq
          Logger.info(s"streaming not yet finished, $remaining of $size items still left to process")
          (0 to 10).foreach(i => Logger.info(streamBuf(i)))
          mayBeStreamEnded = Some(e)
          streamEndLatch.await()
          finishStreaming()
        }
    }
  }

  private def useLock[T](body: => T): T ={
    lock.lock()
    try{
      body
    }finally {
      lock.unlock()
    }
  }

  private def logState(e: Event) = {
  //  Logger.info(s"event: ${e.getClass.getSimpleName} => currSeq: $currSeq, buffer: ${streamBuf.map(_.seqId)}, output: $mayBeOut, streamEnd: $mayBeStreamEnded")
  }

  private def processBuffer(): Unit = {
    Logger.info(s" buffer size: ${streamBuf.size}")
   while(streamBuf.exists(_.seqId == currSeq)){
     val stream = streamBuf.find(_.seqId == currSeq).get
     write(stream)
     streamBuf = streamBuf -= stream
   }
    mayBeStreamEnded.foreach{ e =>
     if(currSeq >= e.size){
       streamEndLatch.countDown()
     }
    }
  }

  private def write(stream: StreamPart): Unit = {
    mayBeOut.get.write(stream.chunk)
    currSeq = stream.seqId
    Logger.debug(s"processed $currSeq chunks")
  }

  private def finishStreaming(): Unit ={
    mayBeOut.foreach { out =>
      out.flush()
      out.close()
    }
    mayBeOut = None
    mayBeStreamEnded = None
    currSeq = 0L
    Logger.info("finished processing streams")
  }
}
