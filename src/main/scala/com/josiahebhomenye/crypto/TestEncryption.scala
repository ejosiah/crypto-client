package com.josiahebhomenye.crypto

import java.io.{ByteArrayInputStream, File}
import java.nio.file.{Files, Paths}
import javax.crypto.{Cipher, KeyGenerator}

import com.cryptoutility.protocol.Events.{StreamEnded, StreamPart, StreamStarted}
import com.cryptoutility.protocol.crypto.{Base64Encode, Encrypt}
import com.josiahebhomenye.crypto.remote.StreamWriter

/**
  * Created by jay on 02/10/2016.
  */
object TestEncryption {

  import Factory._

  val user = extractUserInfo(new File(home))

  val secret = KeyGenerator.getInstance("AES").generateKey()
  val fname = System.getProperty("user.home") + "/Desktop/Screen Shot 2015-09-21 at 09.58.07.png"
  val fname1 = System.getProperty("user.home") + "/Desktop/Screen Shot 2015-09-21 at 09.58.07-2.png"
//  val file = new File(fname)
//  val cipher = Cipher.getInstance(symAlog)
//  cipher.init(Cipher.ENCRYPT_MODE, secret)
//
//  val buf = Files.readAllBytes(file.toPath)
//  val encrypted = cipher.doFinal(buf)
//
//  val cipher1 = Cipher.getInstance(symAlog)
//  cipher.init(Cipher.DECRYPT_MODE, secret)
//  val decrypted = cipher.doFinal(encrypted)
//
//  Files.copy(new ByteArrayInputStream(decrypted), Paths.get(fname1))
  val wrap = Encrypt.wrap(asymAlgo, user.key, Base64Encode(_))(_)
  val encrypt = Encrypt(symAlog, Base64Encode(_))(_, _)

  val writer = new StreamWriter(unwrap, decrypt)
  val wrappedSecret = wrap(secret)
  val filename = encrypt("Screen Shot 2015-09-21 at 09.58.07.png".getBytes(), secret)
  val contentType = encrypt("image/png".getBytes(), secret)
  val from = encrypt("test user".getBytes(), secret)
  val start = StreamStarted(filename, contentType, from, wrappedSecret)
  writer.write(start)
  val clear = Files.readAllBytes(Paths.get(fname))
  val cipher = Cipher.getInstance(symAlog)
  cipher.init(Cipher.ENCRYPT_MODE, secret)
  val cipherText = cipher.doFinal(clear)

  val in = new ByteArrayInputStream(cipherText)
  val buf = new Array[Byte](256)
  var i = 0
  var read = in.read(buf)
  while(read != -1){
    val chunk = new Array[Byte](read)
    Array.copy(buf, 0, chunk, 0, read)
    writer.write(StreamPart(i, chunk))
    read = in.read(buf)
    i = i + 1
  }
  val end = StreamEnded(i, "")
  writer.write(end)


}
