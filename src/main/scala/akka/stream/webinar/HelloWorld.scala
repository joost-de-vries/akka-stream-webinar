package akka.stream.webinar

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.FlexiMerge.{ InputHandle, MergeLogic, ReadAll, ReadAllInputs }
import akka.stream.scaladsl.{ FlexiMerge, FlowGraph, FlowGraphImplicits, Sink, Source }
import scala.io.StdIn._

import scala.collection.immutable.IndexedSeq

object HelloWorld extends App {

  implicit val sys = ActorSystem()
  implicit val mat = FlowMaterializer()

  import sys.dispatcher

  // Element types

  val completion = Source("Hello world".toList)
    .map(_.toUpper)
    .concat(Source("!!!".toList))
    .runWith(Sink.foreach(print(_)))

  completion.onComplete {
    case _ ⇒
      println()
      sys.shutdown()
  }

  readLine()
  sys.shutdown()
  import concurrent.duration._
  sys.awaitTermination(1.second)

}
