package com.josiahebhomenye.crypto

import java.io.{IOException, File}
import java.nio.file.{Files, FileVisitResult, SimpleFileVisitor, Path}
import java.nio.file.attribute.BasicFileAttributes

object PathVisitor{

  def apply(visit: Path => Unit, postVisit: Path => Unit) = new SimpleFileVisitor[Path]{
    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      visit(file)
      FileVisitResult.CONTINUE
    }

    override def postVisitDirectory(dir: Path, e: IOException): FileVisitResult = {
      if(e == null) {
        postVisit(dir)
        FileVisitResult.CONTINUE
      }
      else throw e
    }
  }
}
object Utility {

  def delete(file: File) = {
    val delete = (p: Path) => Files.delete(p)
    Files.walkFileTree(file.toPath, PathVisitor(delete, delete))
  }
}

