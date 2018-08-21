# FS2 KPL (under construction)

_FS2 and Cats effect bindings for the AWS Kinesis KPL library_

Forked from [here](https://github.com/StreetContxt/kpl-scala)

AWS KPL (Kinesis Producer Library) for Scala implemented atop FS2 Streams and Cats Effect. 

`fs2-kpl` provides access to the KPL library using `cats.effect.Resource` or `fs2.Stream` which 
automatically handles resource management. 

The vanilla AWS KPL library is very simple providing just `send` and `shutdown`