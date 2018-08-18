package com.contxt.kinesis.algebras

import java.nio.ByteBuffer

import com.amazonaws.services.kinesis.producer.UserRecordResult

import scala.language.higherKinds

trait ScalaKinesisProducer[F[_]] {
  /** Sends a record to a stream. See
    * [[[com.amazonaws.services.kinesis.producer.KinesisProducer.addUserRecord(String, String, String, ByteBuffer):ListenableFuture[UserRecordResult]*]]].
    */
  def send(streamName: String, partitionKey: String, data: ByteBuffer, explicitHashKey: Option[String] = None): F[UserRecordResult]

  /** Performs an orderly shutdown, waiting for all the outgoing messages before destroying the underlying producer. */
  def shutdown(): F[Unit]
}
