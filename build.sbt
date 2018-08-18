organization in ThisBuild := "com.streetcontxt"
scalaVersion in ThisBuild := "2.11.12"
crossScalaVersions in ThisBuild := Seq("2.11.12", "2.12.6")
licenses in ThisBuild += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
bintrayOrganization in ThisBuild := Some("streetcontxt")

name := "kpl-scala"

val versionPattern = "release-([0-9\\.]*)".r
version := sys.props
  .get("CIRCLE_TAG")
  .orElse(sys.env.get("CIRCLE_TAG"))
  .flatMap { 
    case versionPattern(v) => Some(v)
    case _ => None
  }
  .getOrElse("LOCAL-SNAPSHOT")

libraryDependencies ++= {
  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.21"
  val amazonKinesisProducer = "com.amazonaws" % "amazon-kinesis-producer" % "0.12.9"
  val catsEffect = "org.typelevel" %% "cats-effect" % "0.10.1"

  Seq(slf4j, amazonKinesisProducer, catsEffect)
}
