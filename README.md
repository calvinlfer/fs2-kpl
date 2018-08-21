# Scala KPL FP

_FS2 and Cats effect bindings for the AWS Kinesis KPL library_

Forked from [here](https://github.com/StreetContxt/kpl-scala)

AWS KPL (Kinesis Producer Library) for Scala implemented atop FS2 Streams and Cats Effect. 

`fs2-kpl` provides access to the KPL library using `cats.effect.Resource` or `fs2.Stream` which 
automatically handles resource management. 

## Overview 

The vanilla AWS KPL library is very simple providing just `addUserRecord` (called `send` in the Scala API) to send 
messages to Kinesis and `shutdown` relying on the user to safely perform a shutdown when the KPL resource is no longer 
needed. Along, with this, a few APIs to obtain metrics and for advanced usage, a `flush` to forcefully send all 
aggregated records that are being buffered. The main problem with the vanilla library is that it provides Guava 
ListenableFutures which are not easy to work with from Scala and the vanilla API commits to a concrete effect too early. 

This library takes a Tagless Final approach and allows the user decide which effect they want to use (Cats Effect IO, 
Monix Task, ScalaZ ZIO) provided that the selected effect type has the typeclass constraints enforced by 
Cats Effect typeclasses. 

## Examples

### Resource API

You can safely obtain access via `cats.effect.Resource` and the abstraction will take care of shutting down the KPL and
cleaning up resources

```tut:book:silent
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import cats.effect._
import com.amazonaws.services.kinesis.producer._
import com.github.calvinlfer.fs2.kpl.ScalaKinesisProducer

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

program.map(_ => ExitCode.Success)
```

### Streams API

You can also safely obtain access via `fs2.Stream` if you are working with FS2 Streams and need to set up a streaming
pipeline that needs to publish data to Kinesis

```tut:book:silent
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import cats.effect._
import com.amazonaws.services.kinesis.producer._
import fs2._
import com.github.calvinlfer.fs2.kpl._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

def producerConfig: KinesisProducerConfiguration = {
  val config = new KinesisProducerConfiguration()
  config.setRegion("us-west-2")
  config.setAggregationEnabled(true)
  config.setAggregationEnabled(true)
  config.setMetricsLevel("summary")
  config
}

def mkUserRecord(content: String): UserRecord = {
  new UserRecord("calvin-test-stream", "1", ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)))
}

def pipeline[F[_]: Sync: Timer](kinesis: ScalaKinesisProducer[F]): Stream[F, Unit] = {
  Stream
    .awakeEvery[F](1.second)
    .evalMap(_ => kinesis.send(mkUserRecord("hello")))
    .to(inputStream =>
      inputStream.evalMap[F, Unit](recordResult =>
        Sync[F].delay(
          println(s"${recordResult.isSuccessful}, ${recordResult.getAttempts.size()}, ${recordResult.getShardId}"))))
}

def program[F[_]: Async: Timer]: Stream[F, Unit] = {
  for {
    kinesis <- ScalaKinesisProducer.stream[F](producerConfig)
    _       <- pipeline(kinesis)
  } yield ()
}

val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
implicit val contextShiftIO: ContextShift[IO] = IO.contextShift(ec)
implicit val timerIO: Timer[IO] = cats.effect.IO.timer(ec)

program[IO].compile.drain
```

### Integration with the external world

You can also use this library if you are working in a non-functional style, See 
[here](https://github.com/calvinlfer/scala-kpl-fp/blob/master/examples/src/main/scala/usage/TalkingToExternalWorld.scala) 
for an example. We make use of an `fs2.async.mutable.Queue` to bridge FS2 Streams with the external world.