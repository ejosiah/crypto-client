package com.josiahebhomenye.crypto

import java.io.{PipedInputStream, PipedOutputStream}

/**
  * Created by jay on 02/10/2016.
  */
class PipedStream extends Pipe[PipedOutputStream, PipedInputStream, Byte] {

  override val out = new PipedInputStream()
  override val in = new PipedOutputStream(out)

  def feed(bytes: Array[Byte]): Unit = in.write(bytes)

  def feed(bytes: Array[Byte], from: Int, length: Int) = in.write(bytes, from, length)

  override def close(): Unit = in.close()

  def retrieve(n: Int): Array[Byte] = {
    val buf = new Array[Byte](n)
    out.read(buf)
    buf
  }
}
