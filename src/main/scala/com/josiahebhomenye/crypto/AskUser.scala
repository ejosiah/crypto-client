package com.josiahebhomenye.crypto

import java.util.Scanner

import com.cryptoutility.protocol.Events.UserInfo

import scala.concurrent.{ExecutionContext, Future}

object AskUser {

  def fromConsole()(implicit ec: ExecutionContext) = Future{
    val in = new Scanner(System.in)
    println("Enter your firstName:")
    val fname = in.nextLine()

    println("Enter your lastName:")
    val lname = in.nextLine()

    println("Enter your emailAddress:")
    val email = in.nextLine()

    UserInfo(fname, lname, email)
  }

  def fromGUI()(implicit ec: ExecutionContext) = ???
}
