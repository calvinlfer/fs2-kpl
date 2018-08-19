package com.contxt.kinesis.algebras

import java.nio.ByteBuffer

import com.amazonaws.services.kinesis.producer.UserRecordResult

import scala.language.higherKinds

trait ScalaKinesisProducer[F[_]] {
  /** Sends a record to a stream. See
    * [[[com.amazonaws.services.kinesis.producer.KinesisProducer.addUserRecord(String, String, String, ByteBuffer):ListenableFuture[UserRecordResult]*]]].
    */
  def send(streamName: String, partitionKey: String, data: ByteBuffer, explicitHashKey: Option[String] = None): F[UserRecordResult]

  /**
    * Performs an orderly shutdown, waiting for all the outgoing messages before destroying the underlying producer.
    *
    * I can't define shutdown() as protected and widen it to public (due to SI-1352) to allow the user to manage it for
    * the unsafe use-case and then for all other safe abstractions (Resource and FS2 Stream) keep it protected so I have
    * to make this public and expose this in all safe (where I don't want this) and non-safe abstractions
    */
  def shutdown(): F[Unit]
}
