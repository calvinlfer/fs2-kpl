package com.contxt.kinesis.interpreters

import java.nio.ByteBuffer

import cats.Monad
import cats.effect._
import cats.syntax.all._
import com.amazonaws.services.kinesis.producer.{KinesisProducer, KinesisProducerConfiguration, UserRecordResult}
import com.contxt.kinesis.algebras.ScalaKinesisProducer
import com.google.common.util.concurrent.{FutureCallback, Futures}

import scala.collection.JavaConversions._
import scala.language.{higherKinds, implicitConversions}


// Interpreter
private class ScalaKinesisProducerImpl[F[_]: Async: Monad](private val producer: KinesisProducer) extends ScalaKinesisProducer[F] {
  def send(streamName: String, partitionKey: String, data: ByteBuffer, explicitHashKey: Option[String]): F[UserRecordResult] =
    Async[F].async { callback =>
      val listenableFuture = producer.addUserRecord(streamName, partitionKey, explicitHashKey.orNull, data)
        Futures.addCallback(listenableFuture, new FutureCallback[UserRecordResult] {
          override def onSuccess(result: UserRecordResult): Unit =
            if (result.isSuccessful) callback(Right(result)) else callback(Left(sendFailedException(result)))

          override def onFailure(t: Throwable): Unit = callback(Left(t))
        })
    }

  def shutdown(): F[Unit] = shutdownOnce

  private lazy val shutdownOnce: F[Unit] = for {
    _ <- flushAll()
    _ <- destroyProducer()
  } yield ()

  private def sendFailedException(result: UserRecordResult): RuntimeException = {
    val attemptCount = result.getAttempts.size
    val errorMessage = result.getAttempts.lastOption.map(_.getErrorMessage)
    new RuntimeException(
      s"Sending a record failed after $attemptCount attempts, last error message: $errorMessage."
    )
  }

  private def flushAll(): F[Unit] = Async[F].delay(producer.flushSync())

  private def destroyProducer(): F[Unit] = Async[F].delay(producer.destroy())
}

object ScalaKinesisProducer {
  def apply[F[_]: Async: Monad](kplConfig: KinesisProducerConfiguration): F[ScalaKinesisProducer[F]] = Async[F].delay {
    val producer = new KinesisProducer(kplConfig) // this side-effects
    new ScalaKinesisProducerImpl[F](producer)
  }
}