package com.josiahebhomenye.crypto

import com.typesafe.config.Config

import scala.util.Try


sealed trait Env{
  override def toString = this.getClass.getSimpleName.replace("$", "").toLowerCase()
}

case object Test extends Env
case object Dev extends Env
case object Uat extends Env
case object Staging extends Env
case object Prod extends Env


object Env{

  val current = Env(Option(System.getProperty("env")))

  def apply(value: Option[String]) = value match {
    case Some(env) => env match {
      case "test" => Test
      case "dev" => Dev
      case "uat" => Uat
      case "Staging" => Staging
      case "prod" => Prod
    }
    case None => Prod
  }

  def getString(key: String)(implicit config: Config): String = {
    safeOp(config.getString(s"$current.$key")).getOrElse(config.getString(key))
  }

  def getBoolean(key: String)(implicit config: Config): Boolean = {
    safeOp(config.getBoolean(s"$current.$key")).getOrElse(config.getBoolean(key))
  }

  def getInt(key: String)(implicit config: Config): Int = {
    safeOp(config.getInt(s"$current.$key")).getOrElse(config.getInt(key))
  }

  def getInt(key: String, default: Int)(implicit config: Config): Int = {
    safeOp(getInt(key)).getOrElse(default)
  }

  def getString(key: String, default: String)(implicit config: Config): String = {
    safeOp(getString(key)).getOrElse(default)
  }

  def safeOp[T](get: => T): Option[T] = Try(get).toOption


}