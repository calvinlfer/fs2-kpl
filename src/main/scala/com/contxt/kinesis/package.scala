package com.contxt

import java.nio.ByteBuffer

import cats.Monad
import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.amazonaws.services.kinesis.producer.{KinesisProducer, KinesisProducerConfiguration, UserRecordResult}
import com.contxt.kinesis.algebras.ScalaKinesisProducer
import com.contxt.kinesis.interpreters.ScalaKinesisProducerImpl
import fs2._

import scala.language.higherKinds

package object kinesis {
  object ScalaKinesisProducer {
    // unsafe means that you the user will provide resource management (you will call shutdown only once when/if your program terminates)
    def unsafe[F[_]: Async: Monad](kplConfig: KinesisProducerConfiguration): F[ScalaKinesisProducer[F]] = Async[F].delay {
      val producer = new KinesisProducer(kplConfig) // this side-effects
      new ScalaKinesisProducerImpl[F](producer)
    }

    def resource[F[_]](kplConfig: KinesisProducerConfiguration)(implicit F: Async[F]): Resource[F, ScalaKinesisProducer[F]] = {
      val acquire: F[ScalaKinesisProducer[F]] = F.delay {
        val producer = new KinesisProducer(kplConfig) // this side-effects
        new ScalaKinesisProducerImpl[F](producer)
      }

      val release: ScalaKinesisProducer[F] => F[Unit] =
        producer => producer.shutdown()

      Resource.make(acquire)(release)
    }

    def stream[F[_]](kplConfig: KinesisProducerConfiguration)(implicit F: Async[F]): Stream[F, ScalaKinesisProducer[F]] = {
      val internal: F[ScalaKinesisProducer[F]] = F.delay {
        val producer = new KinesisProducer(kplConfig) // this side-effects
        new ScalaKinesisProducerImpl[F](producer)
      }

      // This hackery is done to prevent the user from shooting themselves in the foot accidentally when using a
      // safe abstraction since the safe abstraction will free up resources on your behalf
      val acquire: F[ScalaKinesisProducer[F]] =
        internal.map { impl =>
        new ScalaKinesisProducer[F] {
          override def send(streamName: String, partitionKey: String, data: ByteBuffer, explicitHashKey: Option[String]): F[UserRecordResult] =
            impl.send(streamName, partitionKey, data, explicitHashKey)

          override def shutdown(): F[Unit] = F.delay(())
        }
      }

      val release: ScalaKinesisProducer[F] => F[Unit] =
        producer => internal.map(impl => impl.shutdown())

      Stream.bracket(acquire)(release)
    }
  }
}
