package com.contxt

import cats.Monad
import cats.effect.{Async, Resource}
import com.amazonaws.services.kinesis.producer.{KinesisProducer, KinesisProducerConfiguration}
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

    def resource[F[_]: Async](kplConfig: KinesisProducerConfiguration): Resource[F, ScalaKinesisProducer[F]] = {
      val acquire: F[ScalaKinesisProducer[F]] = Async[F].delay {
        val producer = new KinesisProducer(kplConfig) // this side-effects
        new ScalaKinesisProducerImpl[F](producer)
      }

      val release: ScalaKinesisProducer[F] => F[Unit] =
        producer => producer.shutdown()

      Resource.make(acquire)(release)
    }

    def stream[F[_]: Async](kplConfig: KinesisProducerConfiguration): Stream[F, ScalaKinesisProducer[F]] = {
      val acquire: F[ScalaKinesisProducer[F]] = Async[F].delay {
        val producer = new KinesisProducer(kplConfig) // this side-effects
        new ScalaKinesisProducerImpl[F](producer)
      }

      val release: ScalaKinesisProducer[F] => F[Unit] =
        producer => producer.shutdown()

      Stream.bracket(acquire)(release)
    }
  }
}
