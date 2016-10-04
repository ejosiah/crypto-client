package com.josiahebhomenye.crypto.command

import java.io._
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.{Path, Files, Paths}
import java.security.Key
import java.util.Date
import javax.crypto.{CipherOutputStream, Cipher}

import com.josiahebhomenye.crypto._
import com.typesafe.config.Config
import scala.collection.JavaConverters._

import com.josiahebhomenye.crypto.Factory._

sealed trait Command extends (() => Unit)

object ListFiles{

  def apply(args: Array[String]) = args.toList match {
    case "-v" :: tail => (new ListFiles(tail, true))()
    case list => (new ListFiles(list))()
  }
}

class ListFiles(files: Seq[String] =  Seq(), verbos: Boolean = false) extends Command {
  override def apply(): Unit = {
    val home = config.getString("user.workDir") + "/data"
    val sbr = new StringBuilder()
    if(files.isEmpty){
      Visit[Unit](home)(sbr ++= info(_))
    }else{
      sbr ++= info(Paths.get(home))
    }
    val (perm, own, s, lm, n, c, sn, id) = ("Permissions", "Owner", "size", "Last Modified", "Name", "ContentType", "sender", "id")
    if(verbos){
      printf(f"$perm\t \t$own\t$s%5s $lm%31s $n%40s$c%20s$sn%20s$id%36s\n")
    }else{
      printf(f"$perm\t \t$own\t$s%5s $lm%31s $n%40s\n")
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
    val (_, name, contentType, from, path) = ExtractMetadata(p.toString)
    val owner = Files.getOwner(p).getName
    val size = p.toFile.length()
    val lastMod = new Date(Files.getLastModifiedTime(p).toMillis).toString
    val id = path.substring(path.lastIndexOf("/") + 1)

    if(verbos){
      f"$permissions%s\t1\t$owner%s\t$size%10d $lastMod%30s $name%40s $contentType%20s $from%20s $id%36s\n"
    }else {
      f"$permissions%s\t1\t$owner%s\t$size%10d $lastMod%30s $name%s\n"
    }
  }
}

object Decrypt{

  def apply(args: Array[String]) = args.toList match {
    case Nil => println("Usage: decrypt [-r] files1 file2 ....")
    case "-r" :: files => (new Decrypt(files, false))()
    case files => (new Decrypt(files, true))()
  }
}

class Decrypt(files: Seq[String], keepEncrypted: Boolean = true) extends Command{
  val workDir = config.getString("user.workDir") + "/data"

  val meta = Visit[(Key, String, String, String, String, DataInputStream)](workDir){ p =>
    ExtractMetadata.withStream(p.toString)
  }.filter( it => files.contains(it._2) )

  def apply(): Unit = {
    meta.foreach { file =>
      val (secret, filename, _, _, path, in) = file
      val cipher = Cipher.getInstance(config.getString("cipher.symmetric.algorithm"))
      cipher.init(Cipher.DECRYPT_MODE, secret)
      val buf = new Array[Byte](in.available())
      in.readFully(buf)
      val decrypted = cipher.doFinal(buf)
      val out = new FileOutputStream(new File(workDir, filename))
      out.write(decrypted)
      out.flush()
      Close(out)
      Close(in)
      Logger.info(s"$path decrypted to $filename")
      if (!keepEncrypted) {
        Logger.info(s"deleting $path")
        new File(path).delete()
      }
    }
    println("decryption completed")
  }
}

object Remove{

  def apply(args: Array[String]) = args.toList match {
    case "*" :: _ => (new Remove(Seq()))()
    case files => (new Remove(files))()
  }
}

class Remove(args: Seq[String]) extends Command{

  override def apply(): Unit = ???
}

object Commands{

  def process(args: Array[String]) = {
    args(0) match {
      case "ls" => ListFiles(args.tail)
      case "decrypt" => Decrypt(args.tail)
      case "rm" => Remove(args.tail)
    }
  }
}