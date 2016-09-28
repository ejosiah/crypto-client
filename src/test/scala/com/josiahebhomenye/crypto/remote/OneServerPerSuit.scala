package com.josiahebhomenye.crypto.remote

import org.scalatest.BeforeAndAfterAll

/**
  * Created by jay on 26/09/2016.
  */
trait OneServerPerSuit extends TestServer with  BeforeAndAfterAll{ self: CryptoClientSpec =>

  override protected def beforeAll(): Unit = start()

  override protected def afterAll(): Unit = stop()
}
