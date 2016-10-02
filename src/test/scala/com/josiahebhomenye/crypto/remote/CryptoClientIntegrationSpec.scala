package com.josiahebhomenye.crypto.remote

import java.io._
import java.nio.file.{Files, Paths}

import _root_.io.netty.channel.Channel
import akka.event.EventStream
import com.cryptoutility.protocol.Events.{UserInfo, _}
import com.josiahebhomenye.crypto._
import io.netty.channel.Channel
import com.cryptoutility.protocol.crypto.{Base64Decode, Base64Encode, Decrypt, Encrypt}

import scala.collection.SortedSet
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Random, Try}


class CryptoClientIntegrationSpec extends ClientITestSetup{

  var mayBeUser = Option.empty[UserInfo]
  val symAlgo = "AES/ECB/PKCS5Padding"
  val asymAlgo = "RSA"

  val encrypt = Encrypt(symAlgo, Base64Encode(_))(_, _)
  val encryptFile = Encrypt.file(symAlgo)(_, _)
  val wrap = Encrypt.wrap(asymAlgo, keyPair.getPublic, Base64Encode(_))(_)
  val decrypt = Decrypt.decrypt(Base64Decode(_), new String(_), symAlgo)(_, _)
  val decryptRaw = Decrypt.decrypt0(new String(_), symAlgo)(_, _)
  val unwrap = Decrypt.unwrap(keyPair.getPrivate, asymAlgo, Base64Decode(_))(_)


  val service = new RemoteCryptoService(unwrap, decrypt)


  def bootStrap = new BootStrap(service.initialise, askUser, saveUser, extractUserInfo, notifyUser)
  def handler = new ServerHandler(Seq(service.notify, bootStrap.notify), Seq(service.on))

  def server = new Server(host, port, handlerChain(handler))

  override def beforeEach(): Unit = {
    Try(Utility.delete(new File(config.getString("user.workDir"))))
    super.beforeEach()
    server.run
  }


  override def afterEach(): Unit = {
    mayBeUser = None
    server.stop()
    super.afterEach()
  }

  def saveUser(user: UserInfo, file: File) = {
    mayBeUser = Some(user)
    user
  }

  def askUser() = Future(("John", "Jones", "john.jones@example.com"))

  def extractUserInfo(file: File) = new UserInfo("Jone", "Jones", "john.jones@example.com", keyPair.getPublic, id)

  def handlerChain(handler: ServerHandler)(ch: Channel) = {
    ch.pipeline()
      .addLast(TestCodec)
      .addLast(handler)
  }


  def get() = {
    Thread.sleep(500.milli.toMillis)
    mayBeUser.orNull
  }

  "Crypto client" should {
    "create user on first start" in {
      replyFromServerWhen[Event]{
        case e: Initialized => UserCreated(e.user)
      }
      val user = get()

      user.fname mustBe "John"
      user.lname mustBe "Jones"
      user.email mustBe "john.jones@example.com"
    }

    "not create user when user exists" in {
      new File(config.getString("user.workDir")).mkdir()
      replyFromServerWhen[Event]{
        case e: Initialized => UserCreated(e.user)
      }
      val user = get()

      user mustBe null
    }
  }

  "must be able to receive encrypted data from the server" in {
    // TODO add sequence numbers to the streams for ordering
    val (secret, filename, contentType, from, clearText) =
      await(sendEncryptedFileFromServer().map{ _ => extractReceivedData() })
    val expected = new String(Files.readAllBytes(Paths.get("src/test/resources/poem.txt")))
    secret mustBe secretKey
    filename mustBe "poem.txt"
    contentType mustBe "application/text"
    from mustBe "Alice Lanistar"
    clearText mustBe expected

  }

  def sendEncryptedFileFromServer() = {

    val file = new File("src/test/resources/poem.txt")
    val pubKey = keyPair.getPublic
    val cipherText = encryptFile(file, secretKey)
    val filename = encrypt("poem.txt", secretKey)
    val contentType = encrypt("application/text", secretKey)
    val from = encrypt("Alice Lanistar", secretKey)

    val secret = wrap(secretKey)
//    Logger.info(s"secret: size: ${secret.length}")
    Logger.info(s"secret: $secret")
    sendFromServer[Event](StreamStarted(filename, contentType, from, secret)).flatMap{ _ =>
      val size = cipherText.length / 4
      val leftOver = cipherText.length - (size * 4)
      val parts = (Seq.fill[Int](3)(size) :+ (if (leftOver == 0) size else leftOver)).zipWithIndex
      var pos = 0
      var streams = ArrayBuffer[StreamPart]()
      parts.foreach{ e =>
        val n = e._1
        val i = e._2
        val buf = new Array[Byte](n)
        Array.copy(cipherText, pos, buf, 0, n)
        streams += StreamPart(i, buf)
        pos = pos + n
      }
      streams = Random.shuffle(streams)
      streams.foreach(sendFromServer[Event](_))
      sendFromServer[Event](StreamEnded(4, ""))
    }

  }

  def extractReceivedData() = {
    var file = new File(config.getString("user.workDir") + "/poem.txt.krypt")
    await(Future{
      while(!file.exists()){
        Thread.sleep(50.millis.toMillis)
        file = new File(config.getString("user.workDir") + "/poem.txt.krypt")
      }
    })
    val raw = Files.readAllBytes(file.toPath)
    val in = new DataInputStream(new ByteArrayInputStream(raw))

    val secret = unwrap(in.readUTF())
    val filename =   decrypt(in.readUTF(), secret)
    val contentType = decrypt(in.readUTF(), secret)
    val from =  decrypt(in.readUTF(), secret)

    val n = in.available()
    val buf = new Array[Byte](n)
    in.readFully(buf)
    val clearText = decryptRaw(buf, secret)

    (secret, filename, contentType, from, clearText)
  }
}
