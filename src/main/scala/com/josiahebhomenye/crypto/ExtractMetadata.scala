package com.josiahebhomenye.crypto

import java.io.{InputStream, FileInputStream, DataInputStream}
import java.security.Key

/**
  * Created by jay on 03/10/2016.
  */
object ExtractMetadata {

  import Factory._

  def apply(paths: Seq[String]): Seq[(Key, String, String, String, String)] = paths.map(apply)

  def apply(path: String): (Key, String, String, String, String) = {
    val (secret, filename, contentType, from, p, in) = withStream(path)
    Close(in)
    (secret, filename, contentType, from, p)
  }

  def withStream(paths: Seq[String]): Seq[(Key, String, String, String, String, DataInputStream)] = paths.map(withStream)

  def withStream(path: String): (Key, String, String, String, String, DataInputStream) = {
    val in = new DataInputStream(new FileInputStream(path))
    val secret = unwrap(in.readUTF())
    val filename = decrypt(in.readUTF(), secret)
    val contentType = decrypt(in.readUTF(), secret)
    val from = decrypt(in.readUTF(), secret)

    (secret, filename, contentType, from, path, in)
  }



}
