package com.josiahebhomenye.crypto

import java.io._
import java.security.KeyPairGenerator

import com.cryptoutility.protocol.Events.{UserCreated, UserInfo}
import com.josiahebhomenye.crypto.remote.{ChannelActive, ChannelEvent}
import com.sun.media.sound.InvalidFormatException
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.control.NonFatal

object BootStrap{
  type Initialize = UserInfo => Future[UserCreated]
  type AskUser = () => Future[(String, String, String)]
  type SaveUser = (UserInfo, File) => UserInfo
  type ExtractUserInfo = File => UserInfo

  def save(info: UserInfo, home: File)(implicit ec: ExecutionContext): UserInfo = {
    TryResource(new PrintWriter(new FileOutputStream(new File(home,"info")))){ out =>
      out.println(info.fname)
      out.println(info.lname)
      out.println(info.email)
      out.println(info.clientId)
      info
    }.get
  }

  def get(home: File)(implicit conf: Config) = {
    val lines = Source.fromFile(new File(home, "info")).getLines().toSeq
    if(lines.size != 4) throw new InvalidFormatException(lines.mkString(", "))
    val publicKey = Crypto.extractPublicKey(home)
    UserInfo(lines.head, lines(1), lines(2), publicKey, lines(3))
  }


}

import BootStrap._

class BootStrap(initialise: Initialize, askUser: AskUser, saveUser: SaveUser, extractUserInfo: ExtractUserInfo, path: String)
               (implicit ec: ExecutionContext, conf: Config) {


  def run: Future[UserInfo] = {
    val home = new File(path)
    if (home.exists()) {
      initialise(extractUserInfo(home)).map(_.user)
    } else {
       askUser().flatMap{ info =>
         if(!home.exists()) home.mkdir()
         val gen = KeyPairGenerator.getInstance(Env.getString("cipher.asymmetric.algorithm"))
         gen.initialize(Env.getInt("cipher.asymmetric.key.length"))
       //TODO figure this out val keyPair @ (_, publicKey) = gen.generateKeyPair()
         val keyPair = gen.generateKeyPair
         Crypto.saveKeys(keyPair, home)
         val (fname, lname, email) = info
         val user = UserInfo(fname, lname, email, keyPair.getPublic)

         initialise(user)
       }.map{ created =>
         saveUser(created.user, home)
       }
    }

  }



  def notify(event: ChannelEvent): Future[Unit] = {
    if (event.isInstanceOf[ChannelActive]) {
      val f = run.map(_ => () )

      f.onFailure { case NonFatal(e) => throw e }
      f
    }else Future.successful()
  }
}
