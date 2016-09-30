package com.josiahebhomenye.crypto.remote

import java.io.{FileOutputStream, Writer}
import java.util.concurrent.locks.ReentrantLock

import scala.collection.mutable

/**
  * Created by jay on 30/09/2016.
  */
class StreamWriter(path: String) extends Writer{

  val lock = new ReentrantLock()
  val out = new FileOutputStream(path)
  var streamBuffer = mutable.SortedSet[StreamEvent]()

  override def flush(): Unit = ???

  override def write(cbuf: Array[Char], off: Int, len: Int): Unit = ???

  override def close(): Unit = ???
}
