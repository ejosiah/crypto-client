package com.josiahebhomenye.crypto.remote

import java.io.{File, InputStream, PushbackInputStream}
import java.security.KeyPairGenerator
import javax.crypto.KeyGenerator

import com.cryptoutility.protocol.EventSerializer
import com.cryptoutility.protocol.EventSerializer._
import com.cryptoutility.protocol.Events.{Event, UserInfo}
import com.josiahebhomenye.crypto.BootStrap
import io.netty.channel.{Channel, ChannelPipeline}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps


trait ClientITestSetup extends CryptoClientSpec with OneServerPerTest with ServerMustMatchers{ self =>
  

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    super.afterEach()
  }


  implicit val EventDecoder = (in: InputStream) => {
    val pis = new PushbackInputStream(in, 4)
    val sizeB = new Array[Byte](IntSize)
    pis.read(sizeB)
    val size = readInt(sizeB)
    pis.unread(sizeB)
    val data = new Array[Byte](size)
    pis.read(data)
    EventSerializer.deserialize(data)
  }

  implicit  val encoder = (e: Any) => {
    EventSerializer.serialize(e.asInstanceOf[Event])
  }
  


  def saveUser(user: UserInfo, file: File): UserInfo

  def askUser(): Future[(String, String, String)]

  def extractUserInfo(file: File): UserInfo
  
  def handlerChain(handler: ServerHandler)(ch: Channel): ChannelPipeline
  val keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair()
  val secretKey = KeyGenerator.getInstance("AES").generateKey()


}