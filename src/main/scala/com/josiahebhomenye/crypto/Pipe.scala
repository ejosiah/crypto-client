package com.josiahebhomenye.crypto

/**
  * Created by jay on 02/10/2016.
  */
trait Pipe[+I, +O, T] extends AutoCloseable{

  def in: I
  def out: O

  def feed(ts: Array[T]): Unit

  def retrieve(n: Int): Array[T]
}
