package com.josiahebhomenye.crypto.remote


import java.io.{DataOutputStream, File, FileOutputStream}
import java.nio.file.Files
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.{Key, KeyFactory}

import com.cryptoutility.protocol.Events._
import com.josiahebhomenye.crypto.NettyToScalaHelpers._
import com.josiahebhomenye.crypto.service.CryptoService
import com.typesafe.config.Config
import io.netty.channel.Channel

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

object RemoteCryptoService{
  var maybeKey = Option.empty[Key]

  def apply(unwrap: String => Key, decrypt: (String, Key) => String)(implicit ec: ExecutionContext, conf: Config) =
    new RemoteCryptoService(unwrap, decrypt)(ec, conf)

  def loadKey(implicit conf: Config) = {
    if(maybeKey.isEmpty){
      val path = new File(conf.getString("user.workDir"))
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

  var mayBeConnection = Option.empty[Channel]
  var maybePromise: Option[Any] = None
  var mayBeOut = Option.empty[DataOutputStream]
  var mayBeUser = Option.empty[UserInfo]
  var mayBeStreamEnded = Option.empty[StreamEnded]
  var streamBuf = ArrayBuffer[StreamPart]()
  var currSeq = 0 // TODO use atomic here

 def initialise(userInfo: UserInfo, isNew: Boolean): Future[UserCreated] = {
   require(mayBeConnection.isDefined, "No connection available")
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
      case ChannelActive(ctx) => mayBeConnection = Some(ctx.channel())
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
        if(currSeq < size){
          println(s"streaming not yet finished, ${size - currSeq} items still left to process")
          logState(e)
          mayBeStreamEnded = Some(e)
        }else {
          finishStreaming()
        }
    }
  }

  private def logState(e: Event) = {
    println(s"event: ${e.getClass.getSimpleName} => currSeq: $currSeq, buffer: ${streamBuf.map(_.seqId)}, output: $mayBeOut, streamEnd: $mayBeStreamEnded")
  }

  private def processBuffer(): Unit ={
   while(streamBuf.exists(_.seqId == currSeq)){
     val stream = streamBuf.find(_.seqId == currSeq).get
     write(stream)
     streamBuf = streamBuf -= stream
   }
    mayBeStreamEnded.foreach{ e =>
      if(currSeq >= e.size){
        finishStreaming()
      }
    }
  }

  private def write(stream: StreamPart): Unit = {
    mayBeOut.get.write(stream.chunk)
    currSeq = currSeq + 1
  }

  private def finishStreaming(): Unit ={
    mayBeOut.foreach { out =>
      out.flush()
      out.close()
    }
    mayBeOut = None
    mayBeStreamEnded = None
    currSeq = 0
    println("finished processing streams")
  }
}
