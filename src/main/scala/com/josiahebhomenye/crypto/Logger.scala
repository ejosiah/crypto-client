package com.josiahebhomenye.crypto

import java.util.Date

/**
  * Created by jay on 30/09/2016.
  */
object Logger {

  // TODO wrap real logger

  def info(msg: => Any) = println(s"${new Date()}: ${Thread.currentThread().getName}: $msg")

  def debug(msg: => Any) =  {} ///println(s"${new Date()}: ${Thread.currentThread().getName}: $msg")

  def trace(msg: => Any) = {} // println(s"${new Date()}: ${Thread.currentThread().getName}: $msg")

  def warn(msg: => Any) = println(s"${new Date()}: ${Thread.currentThread().getName}: $msg")

  def error(msg: => String, t: Throwable = null) = {
    println(s"${new Date()}: ${Thread.currentThread().getName}: $msg")
    Option(t).foreach(_.printStackTrace())
  }

//  def info(msg: => Any) = {}
//
//  def debug(msg: => Any) = {}
//
//  def trace(msg: => Any) = {}
//
//  def warn(msg: => Any) = {}
//
//  def error(msg: => String, t: Throwable = null) = {}
}
