package com.josiahebhomenye.crypto

import javax.swing.JOptionPane
import javax.swing.JOptionPane._

import com.cryptoutility.protocol.Events.UserInfo
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}


object NotifyUsr {

  def apply(user: UserInfo)(implicit ec: ExecutionContext, config: Config): UserInfo = {

    val os = config.getString("os.name").toLowerCase
    val serverUrl = {
      val host = Env.getString("server.host")
      val port = Option(Env.getString("server.port", null)).map(it => s":$it").getOrElse("")
      val secure = Env.getBoolean("server.secure")
      if(secure){
        s"https://$host$port"
      }else{
        s"http://$host$port"
      }
    }

    val runtime = Runtime.getRuntime
    os match {
      case name if name.contains("mac") =>
        Runtime.getRuntime.exec(s"open $serverUrl?userId=${user.clientId}")
      case name if name.contains("windows") =>
        val browsers: Seq[String] = Seq("chrome.exe", "firefox.exe", "ie.exe")
        val results = Visit[String]("C:\\") (p => browsers.find(b => p.toString.endsWith(b)).orNull)
        if(results.nonEmpty){
          val browser = results.find(b => b.contains("chrome.exe")).getOrElse(results.head)
          runtime.exec(s"$browser $serverUrl?userId=${user.clientId}")
        }else{
          import JOptionPane._
          showConfirmDialog(null, s"Put this link in your browser: $serverUrl?userId=${user.clientId}"
            ,"Crypto Client", OK_OPTION, INFORMATION_MESSAGE)
        }
      case _ => Logger.info(s"Put this link in your browser: $serverUrl?userId=${user.clientId}")
    }
    user
  }
}
