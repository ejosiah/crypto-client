package com.josiahebhomenye.crypto

import java.io.File

import com.cryptoutility.protocol.Events.UserInfo
import com.josiahebhomenye.crypto.remote.{ServerHandler, Server, RemoteCryptoService}
import Converters._
import io.netty.util.internal.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

import scala.concurrent.{Future, ExecutionContext}

/**
  * Created by jay on 20/09/2016.
  */
object Main {

  def main(args: Array[String]){
    implicit val ec = ExecutionContext.global
    val home = System.getProperty("user.home") + File.separator + ".crypto-utility"

    val host = "localhost"
    val port = 9000
    val cryptoService = new RemoteCryptoService(home)
    val askUser: () => Future[(String, String, String)] = AskUser.fromConsole
    val saveUser: (UserInfo, File) => UserInfo = BootStrap.save
    val initialise = cryptoService.initialise _
    val extractUserInfo = BootStrap.get _
    val bootStrap = new BootStrap(initialise, askUser, saveUser, extractUserInfo, home)
    val handler = new ServerHandler(Seq(cryptoService.notify _, bootStrap.notify _), Seq(cryptoService.on _))
    val server = new Server(host, port, handler)


    server.run.onComplete{ case _ =>
      Runtime.getRuntime.addShutdownHook(new Thread(server.stop()))
    }
  }
}
