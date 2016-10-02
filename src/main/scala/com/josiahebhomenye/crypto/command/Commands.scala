package com.josiahebhomenye.crypto.command

import java.nio.file.attribute.PosixFilePermission
import java.nio.file.{Path, Files, Paths}
import java.util.Date

import com.josiahebhomenye.crypto.Visit
import com.typesafe.config.Config
import scala.collection.JavaConverters._

sealed trait Command extends (() => Unit)

object ListFiles{

  def apply(args: Array[String])(implicit config: Config) = (new ListFiles(args.toSeq)(config))()
}

class ListFiles(files: Seq[String] =  Seq())(implicit config: Config) extends Command {
  override def apply(): Unit = {
    val home = config.getString("user.workDir")
    val sbr = new StringBuilder()
    if(files.isEmpty){
      Visit[Unit](home)(sbr ++= info(_))
    }else{
      sbr ++= info(Paths.get(home))
    }
    println(sbr.toString)
  }

  def info(p: Path) = {
    val permissions = {
      import PosixFilePermission._
      val br = Array.fill[Char](9)('-')
      Files.getPosixFilePermissions(p).asScala.toSeq.foreach{
        case OWNER_READ => br(0) = 'r'
        case OWNER_WRITE => br(1) = 'w'
        case OWNER_EXECUTE => br(2) = 'x'
        case GROUP_READ => br(3) = 'r'
        case GROUP_WRITE => br(4) = 'w'
        case GROUP_EXECUTE => br(5) = 'x'
        case OTHERS_READ => br(6) = 'r'
        case OTHERS_WRITE => br(7) = 'w'
        case OTHERS_EXECUTE => br(8) = 'x'
      }
      br.mkString
    }
    val owner = Files.getOwner(p).getName
    val size = p.toFile.length()
    val lastMod = new Date(Files.getLastModifiedTime(p).toMillis).toString
    val name = p.toFile.getName

    f"$permissions%s\t1\t$owner%s\t$size%10d $lastMod%30s $name%s\n"
  }
}

class Decrypt(files: Seq[String], keepEncrypted: Boolean = true)(implicit config: Config) extends Command{
  override def apply(): Unit = ???
}

object Commands{

  def process(args: Array[String])(implicit config: Config) = {
    args(0) match {
      case "ls" => ListFiles(args.tail)
    }
  }
}