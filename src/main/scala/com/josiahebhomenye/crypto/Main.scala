package com.josiahebhomenye.crypto

import java.io.File

import com.cryptoutility.protocol.Events.UserInfo
import com.josiahebhomenye.crypto.command.Commands
import com.josiahebhomenye.crypto.remote.{ServerHandler, Server, RemoteCryptoService}
import Converters._
import com.typesafe.config.ConfigFactory
import io.netty.util.internal.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

import scala.concurrent.{Future, ExecutionContext}

/**
  * Created by jay on 20/09/2016.
  */
object Main {
  import Factory._

  def main(args: Array[String]){
    if(args.isEmpty) {
      server.run.onComplete { case _ =>
        Runtime.getRuntime.addShutdownHook(new Thread(server.stop()))
      }
    }else{
      Commands.process(args)
    }
  }
}
