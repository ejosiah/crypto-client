package com.josiahebhomenye.crypto.remote

import java.io.{DataOutputStream, FileOutputStream}
import java.security.Key
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

import com.cryptoutility.protocol.Events._
import com.cryptoutility.protocol.crypto.MD5
import com.josiahebhomenye.crypto.{PipedStream, Logger}
import com.typesafe.config.Config

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal
import scala.concurrent.duration._

/**
  * Created by jay on 30/09/2016.
  */
class StreamWriter(unwrap: String => Key, decrypt: (String, Key) => String)(implicit conf: Config, ec: ExecutionContext) {

  val lock = new ReentrantLock()
  val startCondition = lock.newCondition()
  val started = new AtomicBoolean(false)
  val endReceived = new AtomicBoolean(false)
  val endCondition = lock.newCondition()
  var out: DataOutputStream = null
  var pending = mutable.SortedSet[Event]()
  var pos = 0L
  var size = 0L
  var file: String = null
  var pipe: PipedStream = null
  var digest: Future[String] = null

  def write(event: StreamEvent): Option[StreamingResult] = event match {
    case e: StreamStarted =>
      useLock{
        reset()
        val id = UUID.randomUUID().toString
        val secret = unwrap(e.secret)
        val filename = decrypt(e.filename, secret)
        val path = conf.getString("user.workDir") + s"/data/$id"
        file = path
        Logger.info(s" streaming started for $filename")
        out = new DataOutputStream(new FileOutputStream(path))
        out.writeUTF(e.secret)
        out.writeUTF(e.filename)
        out.writeUTF(e.contentType)
        out.writeUTF(e.from)
        started.set(true)
        startCondition.signal()
        None
      }
    case stream @ StreamPart(seqId, chunk) =>
      useLock{
        if(!started.get())
          startCondition.await()
        if(seqId != pos){
          pending += stream
        }else{
          write0(stream)
          processBuffer()
        }
        None
      }
    case e @ StreamEnded(count, checksum) =>
      useLock{
        size = count
        endReceived.set(true)
        if(pos < size){
          Logger.debug(pending.take(10))
          processBuffer()
          if(pos < size){
           val remaining = size - pos
            Logger.debug(s"streaming not yet finished, $remaining of $size items still left to process")
            endCondition.await()
          }
          endStreaming()
        }else{
          endStreaming()
        }
        // TODO rollback file if checksum is different
        val expected = Await.result(digest, 1 second)
  //      require(checksum == expected, "checksum not as expected")
        Some(StreamingResult(count, Try(Done)))
      }
  }

  def endStreaming() = {
    Logger.info(s"finished processing streams for $file")
    flush()
    close()
    reset()
    Future(pipe.close())
  }

  def flush(): Unit = out.flush()

  def reset(): Unit = {
    started.set(false)
    endReceived.set(false)
    out = null
    pending.empty
    pos = 0L
    size = 0L
    pipe = new PipedStream
    digest = Future(MD5(pipe.out))
    digest.onFailure{case NonFatal(e) => e.printStackTrace()}
  }

  def close(): Unit = {
    try{
      // TODO replace with try Resource
    }finally {
      Try(out.close()) match {
        case Failure(NonFatal(e)) => throw e
        case Success(_) =>
      }
    }
  }

  private def useLock[T](body: => T): T = {
    lock.lock()
    try{ body }finally { lock.unlock()}
  }

  private def processBuffer(): Unit = {
    Logger.trace(s" pending: ${pending.size}")
    // TODO change protocol Order to avoid casting
    while(pending.exists(_.asInstanceOf[StreamPart].seqId == pos )){
      val stream = pending.find(_.asInstanceOf[StreamPart].seqId == pos ).get.asInstanceOf[StreamPart]
      write0(stream)
      pending = pending -= stream
    }
    if(endReceived.get() && pos == size){
      Logger.debug("Sending end signal")
      endCondition.signal()
    }
  }

  private def write0(stream: StreamPart): Unit = {
    Logger.debug(s"writing stream ${stream.seqId}")
    out.write(stream.chunk)
    pipe.feed(stream.chunk)
    pos = pos + 1
    Logger.debug(s"processed $pos chunks")
  }

}
