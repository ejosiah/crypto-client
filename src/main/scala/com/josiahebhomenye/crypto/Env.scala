package com.josiahebhomenye.crypto

import com.typesafe.config.Config


sealed trait Env{
  def name = this.getClass.getSimpleName.replace("$", "").toLowerCase()
}

case object Test extends Env
case object Dev extends Env
case object Uat extends Env
case object Staging extends Env
case object Prod extends Env


object Env{

  lazy val current = Env(System.getProperty("env")).getOrElse(Dev)

  def apply(value: String) = value.toLowerCase match {
    case "test" => Some(Test)
    case "dev" => Some(Dev)
    case "uat" => Some(Uat)
    case "Staging" => Some(Staging)
    case "prod" => Some(Prod)
  }

  def getString(key: String)(implicit config: Config): String = {
    get(Seq(Option(config.getString(s"$current.$key"))), config.getString(key) )
  }

  def getInt(key: String)(implicit config: Config): Int = {
    get(Seq(Option(config.getInt(s"$current.$key"))), config.getInt(key) )
  }

  def getInt(key: String, default: Int)(implicit config: Config): Int = {
   get(Seq(Option(config.getInt(key))
      , Option(config.getInt(s"$current.$key"))
    ), default)

  }

  def getString(key: String, default: String)(implicit config: Config): String = {
    get(Seq(Option(config.getString(key))
      , Option(config.getString(s"$current.$key"))
    ), default)
  }

  def get[T](ops: Seq[Option[T]], default: T): T = ops.foldLeft(default)((d, op)  => op.getOrElse(d))

}