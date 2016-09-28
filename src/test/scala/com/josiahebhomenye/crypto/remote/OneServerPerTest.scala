package com.josiahebhomenye.crypto.remote

import scala.language.postfixOps

/**
  * Created by jay on 24/09/2016.
  */
trait OneServerPerTest extends TestServer{ self: CryptoClientSpec =>


  override  def beforeEach(): Unit = start()

  override  def afterEach(): Unit = stop()


}
