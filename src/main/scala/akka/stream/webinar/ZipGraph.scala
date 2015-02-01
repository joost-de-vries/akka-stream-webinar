package akka.stream.webinar

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{ ZipWith, Zip, Broadcast, Flow, FlowGraph, FlowGraphImplicits, Merge, Sink, Source }
import scala.io.StdIn._

import scala.concurrent.Future

object ZipGraph extends App {

  implicit val sys = ActorSystem()
  implicit val mat = FlowMaterializer()

  import akka.stream.webinar.ZipGraph.sys.dispatcher

  // Element types

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
  val outEnriched = Sink.publisher[Enriched]
  val outEnrichedPair = Sink.publisher[(Enriched, Enriched)]

  FlowGraph { implicit b ⇒
    import akka.stream.scaladsl.FlowGraphImplicits._

    val bcast = Broadcast[Intermediate]

    val zip = Zip[Enriched, Enriched]

    in ~> f1 ~> bcast ~> f2 ~> zip.left
    bcast ~> f4 ~> zip.right
    zip.out ~> outEnrichedPair
  }
  // TODO: make it a Source

  // TODO: ZipWith a Person
  //  final case class Person(name: String, age: Int)
  final case class Person(name: String, surname: String, age: Int)

  FlowGraph { implicit b ⇒
    import FlowGraphImplicits._

    //    val zip = ZipWith { (name: String, age: Int) ⇒ Person(name, age) }
    //    Source.single("ktoso") ~> zip.left
    //    Source.single(42)      ~> zip.right
    //                              zip.out ~> Sink.head[Person]

    val zip = ZipWith { Person.apply _ }

    Source.single("ktoso") ~> zip.input1
    Source.single("fish") ~> zip.input2
    Source.single(42) ~> zip.input3
    zip.out ~> Sink.head[Person]
    // TODO mention you can actually make it look like ~> zip.name etc. (FlexiMerge)
  }

  // TODO flexi merge a Person

  // TODO: more complex partial flow graphs

  readLine()
  sys.shutdown()
  import concurrent.duration._
  sys.awaitTermination(1.second)

}
