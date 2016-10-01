package com.josiahebhomenye.crypto

import java.io.File
import javax.swing.JOptionPane

import com.cryptoutility.protocol.Events.UserInfo
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext


object NotifyUsr {

  val windows = Map(
    "chrome" -> "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
    "firefox" -> "C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe")

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

        val browser = windows.find(e => new File(e._2).exists()).map(_._2)

        if(browser.isDefined){
          runtime.exec(s"${browser.get} $serverUrl?userId=${user.clientId}")
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
