package com.josiahebhomenye.crypto.service

import com.cryptoutility.protocol.Events.UserCreated
import com.cryptoutility.protocol.Events.UserInfo

import scala.concurrent.Future

/**
  * Created by jay on 20/09/2016.
  */
trait CryptoService{
  type Id = String

  def initialise(userInfo: UserInfo): Future[UserCreated]

}
