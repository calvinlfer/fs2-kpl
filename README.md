# FS2 KPL (under construction)

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
ListenableFutures which are not easy to work with 