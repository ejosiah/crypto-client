package com.josiahebhomenye.crypto.remote

import java.security.KeyPairGenerator
import java.util.UUID

import com.typesafe.config.ConfigFactory
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

/**
  * Created by jay on 24/09/2016.
  */
abstract class CryptoClientSpec extends WordSpec with MustMatchers with OptionValues with BeforeAndAfterEach{

  System.setProperty("env", "test")

  implicit val config = ConfigFactory.load()

  implicit val timeout = 30 seconds

  def id = UUID.randomUUID().toString

  def await[T](f: Future[T]) = Await.result(f, timeout)

}
