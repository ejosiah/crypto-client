package com.josiahebhomenye.crypto.remote

import java.io.{DataOutputStream, FileOutputStream}
import java.security.Key
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

import com.cryptoutility.protocol.Events._
import com.josiahebhomenye.crypto.Logger
import com.typesafe.config.Config

import scala.collection.mutable
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

/**
  * Created by jay on 30/09/2016.
  */
class StreamWriter(unwrap: String => Key, decrypt: (String, Key) => String)(implicit conf: Config) {

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

  def write(event: StreamEvent): Unit = event match {
    case e: StreamStarted =>
      useLock{
        val secret = unwrap(e.secret)
        val filename = decrypt(e.filename, secret)
        val path = conf.getString("user.workDir") + s"/$filename.krypt"
        file = path
        Logger.info(s" streaming started for $filename")
        out = new DataOutputStream(new FileOutputStream(path))
        out.writeUTF(e.secret)
        out.writeUTF(e.filename)
        out.writeUTF(e.contentType)
        out.writeUTF(e.from)
        started.set(true)
        startCondition.signal()
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
      }
    case e @ StreamEnded(count, _) =>
      useLock{
        size = count
        endReceived.set(true)
        Logger.info(s"size: $size")
        Logger.info(s"pos: $pos")
        Logger.debug(s"pending size: ${pending.size}")
        processBuffer()
        Logger.debug(s"pending size: ${pending.size}")

        if(pos < size){
          Logger.debug(pending.take(10))
          processBuffer()
          if(pos < size){
           val remaining = size - pos
            Logger.info(s"streaming not yet finished, $remaining of $size items still left to process")
            endCondition.await()
          }
          endStreaming()

        }else{
          endStreaming()
        }
      }
  }

  def endStreaming() = {
    Logger.info(s"finished processing streams for $file")
    flush()
    close()
    reset()
  }

  def flush(): Unit = out.flush()

  def reset(): Unit = {
    started.set(false)
    endReceived.set(false)
    out = null
    pending.empty
    pos = 0L
    size = 0L
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
    Logger.info(s" pending: ${pending.size}")
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
    pos = pos + 1
    Logger.debug(s"processed $pos chunks")
  }

}
