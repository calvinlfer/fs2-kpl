package com.github.calvinlfer.fs2.kpl.algebras

import java.nio.ByteBuffer

import com.amazonaws.services.kinesis.producer.{UserRecord, UserRecordResult}

import scala.language.higherKinds

trait ScalaKinesisProducer[F[_]] {

  /** Sends a record to a stream. See
    * [[[com.amazonaws.services.kinesis.producer.KinesisProducer.addUserRecord(String, String, String, ByteBuffer):ListenableFuture[UserRecordResult]*]]].
    */
  def send(streamName: String,
           partitionKey: String,
           data: ByteBuffer,
           explicitHashKey: Option[String] = None): F[UserRecordResult]

  def send(record: UserRecord): F[UserRecordResult]

  /**
    * Flush all Kinesis Stream buffers that the KPL is buffering messages for
    * @return
    */
  def flushBuffers(): F[Unit]

  def flushBuffer(streamName: String): F[Unit]

  /**
    * Performs an orderly shutdown, waiting for all the outgoing messages before destroying the underlying producer.
    *
    * I can't define shutdown() as protected and widen it to public (due to SI-1352) to allow the user to manage it for
    * the unsafe use-case and then for all other safe abstractions (Resource and FS2 Stream) keep it protected so I have
    * to make this public and expose this in all safe (where I don't want this) and non-safe abstractions
    */
  protected[kpl] def shutdown(): F[Unit]
}
