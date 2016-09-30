package com.josiahebhomenye.crypto

import java.io.File

import com.cryptoutility.protocol.Events.UserInfo
import com.josiahebhomenye.crypto.remote.{Server, ServerHandler, RemoteCryptoService}
import com.typesafe.config.ConfigFactory
import com.cryptoutility.protocol.crypto.{Decrypt, Base64Decode}
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try

/**
  * Created by jay on 28/09/2016.
  */
object Factory {

  implicit val ec = ExecutionContext.global
  implicit val config = ConfigFactory.load()
  val home = System.getProperty("user.home") + File.separator + ".crypto-utility"
  val symAlog = config.getString("cipher.symmetric.algorithm")
  val asymAlgo = config.getString("cipher.asymmetric.algorithm")
  lazy val loadKey = RemoteCryptoService.loadPrivateKey
  val decrypt = Decrypt.decrypt(Base64Decode(_), new String(_), symAlog)(_, _)
  val unwrap = Decrypt.unwrap(loadKey, asymAlgo, Base64Decode(_))(_)
  val host = Env.getString("server.host")
  val port = Try(Env.getInt("server.port")).orElse(Try(-1)).get
  val cryptoService = new RemoteCryptoService(unwrap, decrypt)
//  val askUser: () => Future[(String, String, String)] = AskUser.fromConsole
  val askUser = () => Future.successful(("Josiah", "Ebhomenye", "jebhomenye@yahoo.com"))
  val saveUser: (UserInfo, File) => UserInfo = BootStrap.save
  val initialise = cryptoService.initialise _
  val extractUserInfo = BootStrap.get _
  val notifyUser = NotifyUsr.apply _
  val bootStrap = new BootStrap(initialise, askUser, saveUser, extractUserInfo, notifyUser)
  val handler = new ServerHandler(Seq(cryptoService.notify _, bootStrap.notify _), Seq(cryptoService.on _))
  val handlerChain = Server.webSocketHandlers(handler, host, port)(_)



  val server = Server(home, host, port, handlerChain)

}
