package com.josiahebhomenye.crypto

import java.io._
import java.nio.file.{Files, StandardCopyOption}
import java.security.{Key, KeyFactory, KeyPair => JKeyPair}
import java.security.spec.X509EncodedKeySpec

object KeyPair{

  def unapply(keyPair: JKeyPair) = Some((keyPair.getPrivate, keyPair.getPublic))
}

object Crypto {


  def readFully(file: File) = {
    val out = new ByteArrayOutputStream(1024)
    Files.copy(file.toPath, out)
    out.toByteArray
  }

  def extractPublicKey(home: File, algorithm: String = "RSA") = {
    val keyPath = new File(home, "user.public.pub")
    if(!keyPath.exists()) throw new RuntimeException("public key not found")  // TODO try to fix this if it happens
    val raw = readFully(keyPath)
    val keySpec = new X509EncodedKeySpec(raw)
    KeyFactory.getInstance(algorithm).generatePublic(keySpec) // TODO externalise algorithm
  }

  def saveKeys(keyPair: JKeyPair, home: File) = {
    saveKey(keyPair.getPrivate, home, "user.private.der")
    saveKey(keyPair.getPrivate, home, "user.public.pub")
  }

  def saveKey(key: Key, home: File, saveAs: String) = {
    val path = new File(home, saveAs).toPath
    Files.copy(new ByteArrayInputStream(key.getEncoded), path, StandardCopyOption.REPLACE_EXISTING)
  }


}
