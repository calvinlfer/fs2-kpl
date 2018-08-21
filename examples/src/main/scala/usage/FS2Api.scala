package usage

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import cats.effect._
import com.amazonaws.services.kinesis.producer.{KinesisProducerConfiguration, UserRecord}
import com.github.calvinlfer.fs2.kpl._
import fs2._

import scala.concurrent.duration._

object FS2Api extends IOApp {
  def producerConfig: KinesisProducerConfiguration = {
    val config = new KinesisProducerConfiguration()
    config.setRegion("us-west-2")
    config.setAggregationEnabled(true)
    config.setAggregationEnabled(true)
    config.setMetricsLevel("summary")
    config
  }

  def mkUserRecord(content: String): UserRecord =
    new UserRecord("calvin-test-stream", "1", ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)))

  def pipeline[F[_]: Sync: Timer](kinesis: ScalaKinesisProducer[F]): Stream[F, Unit] =
    Stream
      .awakeEvery[F](1.second)
      .evalMap(_ => kinesis.send(mkUserRecord("hello")))
      .to(inputStream =>
        inputStream.evalMap[F, Unit](recordResult =>
          Sync[F].delay(
            println(s"${recordResult.isSuccessful}, ${recordResult.getAttempts.size()}, ${recordResult.getShardId}"))))

  def program[F[_]: Async: Timer]: Stream[F, Unit] =
    for {
      kinesis <- ScalaKinesisProducer.stream[F](producerConfig)
      _       <- pipeline(kinesis)
    } yield ()

  override def run(args: List[String]): IO[ExitCode] =
    program[IO].compile.drain.map(_ => ExitCode.Success)
}
