import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import cats.effect.{IO, Resource}
import com.amazonaws.services.kinesis.producer.{KinesisProducerConfiguration, UserRecord}
import com.github.calvinlfer.fs2.kpl._

object ResourceAPIUsage extends App {
  def producerConfig: KinesisProducerConfiguration = {
    val config = new KinesisProducerConfiguration()
    config.setRegion("us-west-2")
    config.setAggregationEnabled(true)
    config.setAggregationEnabled(true)
    config.setMetricsLevel("summary")
    config
  }

  val kplResource: Resource[IO, ScalaKinesisProducer[IO]] = ScalaKinesisProducer[IO](producerConfig)

  def mkUserRecord(content: String): UserRecord =
    new UserRecord("calvin-test-stream", "1", ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)))

  val program: IO[Unit] = kplResource.use { kinesis: ScalaKinesisProducer[IO] =>
    for {
      _ <- kinesis.send(mkUserRecord("hello"))
      _ <- kinesis.send(mkUserRecord("bye!"))
    } yield ()
  }

  program.unsafeRunSync()
}
