package com.josiahebhomenye.crypto

import java.io.{File, IOException}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import scala.util.Try

object Visit{

  def apply[T](path: String, visit: Path => T, postVisit: Path => T, stopOnError: Boolean = false): Seq[T] = {
    val visitor = new Visit(visit, postVisit, stopOnError)
    Files.walkFileTree(Paths.get(path), visitor())
    visitor.result
  }

  def apply[T](path: String)(visit: Path => T): Seq[T]
    = apply(path, visit, (p) => null.asInstanceOf[T])
}

class Visit[T](visit: Path => T, postVisit: Path => T, stopOnError: Boolean = false) {
  val result = Seq.empty[T]

  def apply() = new SimpleFileVisitor[Path] {
    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      Try(visit(file)).toOption.foreach(result :+ _)
      FileVisitResult.CONTINUE
    }

    override def postVisitDirectory(dir: Path, e: IOException): FileVisitResult = {
      if (e == null) {
        Try(postVisit(dir)).toOption.foreach(result :+ _)
        FileVisitResult.CONTINUE
      }
      else if(stopOnError) {
        throw e
      }else{
        FileVisitResult.CONTINUE
      }
    }
  }
}

object Utility {

  def delete(file: File) = {
    val delete = (p: Path) => Files.delete(p)
    Visit(file.getPath, delete, delete)
  }
}

