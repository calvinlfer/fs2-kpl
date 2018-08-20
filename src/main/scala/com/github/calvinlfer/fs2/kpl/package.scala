package com.github.calvinlfer.fs2

import cats.effect.{Async, Resource}
import com.amazonaws.services.kinesis.producer.{KinesisProducer, KinesisProducerConfiguration}
import com.github.calvinlfer.fs2.kpl.algebras.ScalaKinesisProducer
import com.github.calvinlfer.fs2.kpl.interpreters.ScalaKinesisProducerImpl
import fs2._

import scala.language.higherKinds

package object kpl {
  object ScalaKinesisProducer {
    def apply[F[_]: Async](kplConfig: => KinesisProducerConfiguration): Resource[F, ScalaKinesisProducer[F]] = {
      val acquire: F[ScalaKinesisProducer[F]] = Async[F].delay {
        val producer = new KinesisProducer(kplConfig)
        new ScalaKinesisProducerImpl[F](producer)
      }

      val release: ScalaKinesisProducer[F] => F[Unit] =
        producer => producer.shutdown()

      Resource.make(acquire)(release)
    }

    def stream[F[_]: Async](kplConfig: => KinesisProducerConfiguration): Stream[F, ScalaKinesisProducer[F]] = {
      val acquire: F[ScalaKinesisProducer[F]] = Async[F].delay {
        val producer = new KinesisProducer(kplConfig)
        new ScalaKinesisProducerImpl[F](producer)
      }

      val release: ScalaKinesisProducer[F] => F[Unit] =
        producer => producer.shutdown()

      Stream.bracket(acquire)(release)
    }
  }
}
