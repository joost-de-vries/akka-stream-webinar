package akka.stream.webinar

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{ Merge, Broadcast, FlowGraph, FlowGraphImplicits, Sink, Source, Flow }
import scala.io.StdIn._

import scala.concurrent.Future

object SimpleGraph extends App {

  implicit val sys = ActorSystem()
  implicit val mat = FlowMaterializer()

  import sys.dispatcher

  sealed trait Input {
    def toIntermediate = Intermediate
  }
  object Input extends Input

  sealed trait Intermediate {
    def enrich = Enriched
    def enrichAsync = Future(Enriched)

  }
  object Intermediate extends Intermediate

  sealed trait Enriched {
    def isImportant = true
  }
  object Enriched extends Enriched

  val f1 = Flow[Input].map(_.toIntermediate)
  val f2 = Flow[Intermediate].map(_.enrich)
  val f3 = Flow[Enriched].filter(_.isImportant)
  val f4 = Flow[Intermediate].mapAsync(_.enrichAsync)

  val in = Source.subscriber[Input]
  val out = Sink.publisher[Enriched]

  FlowGraph { implicit b ⇒
    import FlowGraphImplicits._

    val bcast = Broadcast[Intermediate]
    val merge = Merge[Enriched]

    in ~> f1 ~> bcast ~> f2 ~> merge
    bcast ~> f4 ~> merge ~> f3 ~> out
  }

  readLine()
  sys.shutdown()
  import concurrent.duration._
  sys.awaitTermination(1.second)
}
