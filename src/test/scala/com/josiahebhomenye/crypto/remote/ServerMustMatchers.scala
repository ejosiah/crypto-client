package com.josiahebhomenye.crypto.remote

import java.io.InputStream

/**
  * Created by jay on 26/09/2016.
  */
trait ServerMustMatchers { self: CryptoClientSpec with TestServer =>

  implicit class AtServer[T](underlying: T){

    def mustBeReceivedAtServer(implicit decoder: InputStream => T)  = assertServerReceived(underlying)
  }

  def assertServerReceived[T](msg: T)(implicit decoder: InputStream => T) = {
    val result = self.await(self.in.map(decoder))
    result mustBe msg
  }

}
