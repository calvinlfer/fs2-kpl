package com.contxt.kinesis

import com.amazonaws.services.kinesis.producer.{ KinesisProducer, KinesisProducerConfiguration, UserRecordResult }
import com.google.common.util.concurrent.ListenableFuture
import java.nio.ByteBuffer
import scala.concurrent._
import scala.language.implicitConversions
import scala.util.Try
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

/** A lightweight Scala wrapper around Kinesis Producer Library (KPL). */
trait ScalaKinesisProducer {
  /** Sends a record to a stream. See
    * [[[com.amazonaws.services.kinesis.producer.KinesisProducer.addUserRecord(String, String, String, ByteBuffer):ListenableFuture[UserRecordResult]*]]].
    */
  def send(streamName: String, partitionKey: String, data: ByteBuffer, explicitHashKey: Option[String] = None): Future[UserRecordResult]

  /** Performs an orderly shutdown, waiting for all the outgoing messages before destroying the underlying producer. */
  def shutdown(): Future[Unit]
}

object ScalaKinesisProducer {
  def apply(
    kplConfig: KinesisProducerConfiguration): ScalaKinesisProducer = {
    val producer = new KinesisProducer(kplConfig)
    new ScalaKinesisProducerImpl(producer)
  }

  private[kinesis] implicit def listenableToScalaFuture[A](listenable: ListenableFuture[A]): Future[A] = {
    val promise = Promise[A]
    val callback = new Runnable {
      override def run(): Unit = promise.tryComplete(Try(listenable.get()))
    }
    listenable.addListener(callback, ExecutionContext.global)
    promise.future
  }
}

private[kinesis] class ScalaKinesisProducerImpl(private val producer: KinesisProducer) extends ScalaKinesisProducer {
  import ScalaKinesisProducer.listenableToScalaFuture

  def send(streamName: String, partitionKey: String, data: ByteBuffer, explicitHashKey: Option[String]): Future[UserRecordResult] =
    producer.addUserRecord(streamName, partitionKey, explicitHashKey.orNull, data).map { result =>
      if (!result.isSuccessful) throwSendFailedException(result) else result }

  def shutdown(): Future[Unit] = shutdownOnce

  private lazy val shutdownOnce: Future[Unit] = {
    val allFlushedFuture = flushAll()
    val shutdownPromise = Promise[Unit]
    allFlushedFuture.onComplete { _ =>
      shutdownPromise.completeWith(destroyProducer())
    }
    allFlushedFuture
      .zip(shutdownPromise.future)
      .map(_ => ())
  }

  private def throwSendFailedException(result: UserRecordResult): Nothing = {
    val attemptCount = result.getAttempts.size
    val errorMessage = result.getAttempts.lastOption.map(_.getErrorMessage)
    throw new RuntimeException(
      s"Sending a record failed after $attemptCount attempts, last error message: $errorMessage."
    )
  }

  private def flushAll(): Future[Unit] = {
    Future {
      blocking {
        producer.flushSync()
      }
    }
  }

  private def destroyProducer(): Future[Unit] = {
    Future {
      blocking {
        producer.destroy()
      }
    }
  }
}
