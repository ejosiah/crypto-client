package com.josiahebhomenye

import java.net.{URI}
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.nio.{NioEventLoopGroup => NettyNioEventLoopGroup}
import io.netty.handler.codec.http.websocketx.{WebSocketClientProtocolHandler => NettyWebSocketClientProtocolHandler, WebSocketVersion}
import io.netty.handler.codec.http._

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

/**
  * Created by jay on 20/09/2016.
  */
package object crypto {
  type ContentType = String
  type Headers = Map[String, String]
  val EmptyHeaders = () => Map.empty[String, String]

  private class CFFuture(cf: ChannelFuture) extends Future[Channel]{

    var mayBeValue: Option[Try[Channel]] = None

    cf.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture): Unit =
        if(future.isSuccess)
          mayBeValue = Some(Success(future.channel()))
        else
          mayBeValue = Some(Failure(future.cause()))

    })

    override def onComplete[U](func: Try[Channel] => U)(implicit executor: ExecutionContext): Unit =
      cf.addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture): Unit = {
          if(future.isSuccess){
            func(Success(future.channel()))
          }else{
            func(Failure(future.cause()))
          }
        }
      })

    override def isCompleted: Boolean = cf.isDone

    override def value: Option[Try[Channel]] = mayBeValue


    @throws[InterruptedException](classOf[InterruptedException])
    @throws[TimeoutException](classOf[TimeoutException])
    override def ready(atMost: Duration)(implicit permit: CanAwait): CFFuture.this.type = {
      if(!cf.await(atMost.toMillis)) throw new TimeoutException
      this
    }

    @throws[Exception](classOf[Exception])
    override def result(atMost: Duration)(implicit permit: CanAwait): Channel =
      if (!cf.await(atMost.toMillis))
        throw new TimeoutException
      else cf.channel()
  }

  object Converters {
    type Entry[A, B] = java.util.Map.Entry[A, B]
    type JSet[E] = java.util.Set[E]
    type JCollection[E] = java.util.Collection[E]

    implicit def convert[T](consume: T => Unit) : Consumer[T] = new Consumer[T](){
      def accept(t: T): Unit = consume(t)
    }

    implicit def collectionOfEntryToSeqOfTuple[A, B](config: JCollection[Entry[A, B]]): Seq[(A, B)] = {
      var result: Seq[(A, B)] = Seq()
      config.forEach{e: Entry[A, B]  => result = result ++ Seq((e.getKey, e.getValue))}
      result
    }
    implicit def convert(func: => Unit): Runnable = new Runnable(){ def run(): Unit = func }

    implicit def convert(runnable: Runnable): Thread = new Thread(runnable)

  }
  object NettyToScalaHelpers{

    implicit def convert(cf: ChannelFuture) : Future[Channel] = new CFFuture(cf)

    implicit def convert[C <: Channel](f: C => ChannelPipeline) : ChannelInitializer[C] = new ChannelInitializer[C] {
      override def initChannel(ch: C): Unit = f(ch)
    }

    implicit def convert(headers: HttpHeaders) : Map[String, String] = {
      var result = Map.empty[String, String]
      Converters.collectionOfEntryToSeqOfTuple(headers.entries()).foreach{ e => result = result ++ Map(e._1 -> e._2) }
      result
    }

    implicit def convert(byteBuf: ByteBuf): Array[Byte] = {
      val data = new Array[Byte](byteBuf.readableBytes())
      byteBuf.readBytes(data)
      data
    }

  }

  implicit class RichHttpRequest(underlying: HttpRequest){

    def if100ContinueExpected(body: => Unit): Unit =
      if(HttpUtil.is100ContinueExpected(underlying)) body

    def ifKeepAlive(body: => Unit): Unit =
      if(HttpUtil.isKeepAlive(underlying)) body
  }

  object NioEventLoopGroup {

    def apply(): NettyNioEventLoopGroup = new NettyNioEventLoopGroup()
  }


  object WebSocketClientProtocolHandler{

    def apply(uri: String, maxFramePayLoadLength: Int) = {
      val noSubProtocol = null
      val allowExtensions = true
      val noCustomHeaders = null
      new  NettyWebSocketClientProtocolHandler(new URI(uri), WebSocketVersion.V13, noSubProtocol, allowExtensions, noCustomHeaders, maxFramePayLoadLength)
    }
  }

  object TryResource{

    def apply[AC <: AutoCloseable, R](ac: AC)(body: AC => R): Try[R] = {
      var mayBeEx = Option.empty[Throwable]
      try{
        val r = body(ac)
        Success(r)
      }catch{
        case e: Throwable =>
          mayBeEx = Some(e)
          Failure(e)
      }finally{
        if(ac != null){
          try{
            ac.close()
          }catch {
            case t: Throwable =>
              if(mayBeEx.isDefined) t.addSuppressed(mayBeEx.get)
              Failure(t)
          }
        }
      }
    }
  }
}
