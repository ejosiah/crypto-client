package com.josiahebhomenye.crypto.command

import com.typesafe.config.Config

sealed trait Command extends (() => ())

class ListFiles(files: Seq[String])(implicit config: Config) extends Command {
  override def apply(): Unit = ???
}

class Decrypt(files: Seq[String], keepEncrypted: Boolean = true)(implicit config: Config) extends Command{
  override def apply(): Unit = ???
}