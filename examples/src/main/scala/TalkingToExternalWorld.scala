import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import cats.Monad
import cats.syntax.all._
import cats.effect.{Async, ContextShift, IO}
import com.amazonaws.services.kinesis.producer.{KinesisProducerConfiguration, UserRecordResult}
import com.github.calvinlfer.fs2.kpl.ScalaKinesisProducer
import com.github.calvinlfer.fs2.kpl.algebras.ScalaKinesisProducer
import fs2._
import fs2.async.mutable.Queue

object TalkingToExternalWorld extends App {
  def producerConfig: KinesisProducerConfiguration = {
    val config = new KinesisProducerConfiguration()
    config.setRegion("us-west-2")
    config
  }

  // FS2 pipeline that consumes the data
  def pipeline[F[_]: Async](q: Queue[F, String]): Stream[F, Unit] = {
    def printSink(queue: Queue[F, String], kinesis: ScalaKinesisProducer[F]): Stream[F, Unit] =
      queue.dequeue
        .evalMap[F, UserRecordResult](message =>
          kinesis.send("calvin-test-stream", "1", ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8))))
        .evalMap[F, Unit](result => Async[F].delay(println(result.isSuccessful)))

    val streamKinesis: Stream[F, ScalaKinesisProducer[F]] = ScalaKinesisProducer.stream[F](producerConfig)
    for {
      kinesis <- streamKinesis
      _       <- printSink(q, kinesis)
    } yield ()
  }

  // initialize in the impure area
  implicit val contextShiftIO: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)
  val streamQueue: IO[Queue[IO, String]]        = fs2.async.boundedQueue[IO, String](1)
  val queue                                     = streamQueue.unsafeRunSync()

  // FS2 pipeline description
  val program: Stream[IO, Unit] = pipeline(queue)
  // Run FS2 pipeline
  program.compile.drain.unsafeToFuture()
  println("running pipeline")

  def queueEnqueueProgram[F[_]: Monad](queue: Queue[F, String]): F[Unit] =
    for {
      _ <- queue.enqueue1("hello")
      _ <- queue.enqueue1("hi")
      _ <- queue.enqueue1("bye")
      _ = println("iteration complete")
    } yield ()

  // send data to FS2 pipeline from external world
  queueEnqueueProgram[IO](queue).unsafeRunSync() // side-effects!
  Thread sleep 5000L
}
