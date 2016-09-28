package com.josiahebhomenye.crypto

import java.io._
import java.nio.file.{Files, StandardCopyOption}
import java.security.{KeyPair => JKeyPair, PrivateKey, PublicKey, Key, KeyFactory}
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

import com.typesafe.config.Config

object Crypto {

  def readFully(file: File) = {
    val out = new ByteArrayOutputStream(1024)
    Files.copy(file.toPath, out)
    out.toByteArray
  }

  def extractPublicKey(home: File)(implicit conf: Config) = {
    val algorithm = Env.getString("cipher.asymmetric.algorithm")
    val keyPath = new File(home, Env.getString("user.key.public"))
    if(!keyPath.exists()) throw new RuntimeException("public key not found")  // TODO try to fix this if it happens
    val raw = readFully(keyPath)
    val keySpec = new X509EncodedKeySpec(raw)
    KeyFactory.getInstance(algorithm).generatePublic(keySpec)
  }

  def saveKeys(keyPair: JKeyPair, home: File)(implicit conf: Config) = {
    saveKey(keyPair.getPrivate, home, Env.getString("user.key.private"))
    saveKey(keyPair.getPrivate, home, Env.getString("user.key.public"))
  }

  def saveKey(key: Key, home: File, saveAs: String) = {
    val path = new File(home, saveAs).toPath
    Files.copy(new ByteArrayInputStream(key.getEncoded), path, StandardCopyOption.REPLACE_EXISTING)
  }

}
