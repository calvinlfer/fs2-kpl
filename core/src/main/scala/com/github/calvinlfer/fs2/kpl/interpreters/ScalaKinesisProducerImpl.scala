package com.github.calvinlfer.fs2.kpl.interpreters

import java.nio.ByteBuffer

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.amazonaws.services.kinesis.producer.{KinesisProducer, UserRecordResult}
import com.github.calvinlfer.fs2.kpl.algebras.ScalaKinesisProducer
import com.google.common.util.concurrent.{FutureCallback, Futures}

import scala.collection.JavaConverters._
import scala.language.higherKinds

private[kpl] class ScalaKinesisProducerImpl[F[_]](private val producer: KinesisProducer)(implicit A: Async[F])
    extends ScalaKinesisProducer[F] {
  def send(streamName: String,
           partitionKey: String,
           data: ByteBuffer,
           explicitHashKey: Option[String]): F[UserRecordResult] =
    A.async { callback =>
      val listenableFuture = producer.addUserRecord(streamName, partitionKey, explicitHashKey.orNull, data)
      Futures.addCallback(
        listenableFuture,
        new FutureCallback[UserRecordResult] {
          override def onSuccess(result: UserRecordResult): Unit =
            if (result.isSuccessful) callback(Right(result)) else callback(Left(sendFailedException(result)))

          override def onFailure(t: Throwable): Unit = callback(Left(t))
        }
      )
    }

  def shutdown(): F[Unit] =
    for {
      _ <- flushAll()
      _ <- destroyProducer()
    } yield ()

  private def sendFailedException(result: UserRecordResult): RuntimeException = {
    val attemptCount = result.getAttempts.size
    val errorMessage = result.getAttempts.asScala.lastOption.map(_.getErrorMessage)
    new RuntimeException(
      s"Sending a record failed after $attemptCount attempts, last error message: $errorMessage."
    )
  }

  private def flushAll(): F[Unit] = A.delay(producer.flushSync())

  private def destroyProducer(): F[Unit] = A.delay(producer.destroy())
}
