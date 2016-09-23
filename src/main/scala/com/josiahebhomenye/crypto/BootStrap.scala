package com.josiahebhomenye.crypto

import java.io.{FileOutputStream, PrintWriter, File}
import java.security.KeyPairGenerator

import com.cryptoutility.protocol.Events.UserCreated
import com.cryptoutility.protocol.Events.UserInfo
import com.josiahebhomenye.crypto.remote.{ChannelActive, ChannelEvent}
import com.josiahebhomenye.crypto.service.CryptoService
import com.sun.media.sound.InvalidFormatException
import io.netty.channel.ChannelHandlerContext

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import com.josiahebhomenye.crypto._

import scala.util.control.NonFatal
import scala.util.{Failure, Success}


import java.security.{KeyPair => JKeyPair }

object KeyPair{

  def unapply(keyPair: JKeyPair) = Some((keyPair.getPrivate, keyPair.getPublic))
}

object BootStrap{
  type Initialize = UserInfo => Future[UserCreated]
  type AskUser = () => Future[(String, String, String)]
  type SaveUser = (UserInfo, File) => UserInfo

  def save(info: UserInfo, home: File)(implicit ec: ExecutionContext): UserInfo = {
    if(!home.exists()) home.mkdir()
    TryResource(new PrintWriter(new FileOutputStream(new File(home,"info")))){ out =>
      out.println(info.fname)
      out.println(info.lname)
      out.println(info.email)
      out.println(info.clientId.get)
      info
    }.get
  }
}

import BootStrap._

class BootStrap(initialise: Initialize, askUser: AskUser, saveUser: SaveUser, path: String)(implicit ec: ExecutionContext) {


  def run: Future[UserInfo] = {
    val home = new File(path)
    if (home.exists()) {
      initialise(extractUserInfo(home)).map(_.user)
    } else {
       askUser().flatMap{ info =>
         val gen = KeyPairGenerator.getInstance("RSA")
         gen.initialize(2048)
         val (privateKey, publicKey) = gen.generateKeyPair()
         // TODO save keys
         val (fname, lname, email) = info
         val user = UserInfo(fname, lname, email, publicKey)

         initialise(user)
       }.map{ created =>
         saveUser(created.user, home)
       }
    }

  }

  private def extractUserInfo(home: File) = { // TODO move out of here
    val lines = Source.fromFile(new File(home, "info")).getLines().toSeq
    if(lines.size != 4) throw new InvalidFormatException(lines.mkString(", "))
    UserInfo(lines.head, lines(1), lines(2), null, Some(lines(3)))
  }

  def notify(event: ChannelEvent): Future[Unit] = {
    if (event.isInstanceOf[ChannelActive]) {
      val f = run.map(_ => () )

      f.onFailure { case NonFatal(e) => throw e }
      f
    }else Future.successful()
  }
}
