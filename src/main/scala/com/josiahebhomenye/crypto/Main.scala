package com.josiahebhomenye.crypto

import com.josiahebhomenye.crypto.Converters._
import com.josiahebhomenye.crypto.command.Commands

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
