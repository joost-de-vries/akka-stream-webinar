package akka.stream.webinar

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.FlexiMerge.{ ReadAllInputs, ReadAll, InputHandle, MergeLogic }
import akka.stream.scaladsl.{ FlexiMerge, Broadcast, Flow, FlowGraph, FlowGraphImplicits, Sink, Source, Zip, ZipWith }
import scala.io.StdIn._

import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future

object FlexiMergePerson extends App {

  implicit val sys = ActorSystem()
  implicit val mat = FlowMaterializer()

  import akka.stream.webinar.FlexiMergePerson.sys.dispatcher

  // Element types

  // TODO: FlexiMerge a person

  final case class Person(name: String, surname: String, age: Int)

  class PersonMerge extends FlexiMerge[Person] {
    val name = createInputPort[String]()
    val surname = createInputPort[String]()
    val age = createInputPort[Int]()

    override def createMergeLogic(): MergeLogic[Person] = new MergeLogic[Person] {

      override def inputHandles(inputCount: Int): IndexedSeq[InputHandle] =
        Vector(name, surname, age)

      override def initialState = State[ReadAllInputs](ReadAll(name, surname, age)) { (ctx, _, inputs) ⇒
        ctx.emit(Person(inputs(name), inputs(surname), inputs(age)))
        SameState
      }

    }
  }

  FlowGraph { implicit b ⇒
    import FlowGraphImplicits._

    val zip = new PersonMerge

    Source.single("ktoso") ~> zip.name
    Source.single("fish") ~> zip.surname
    Source.single(42) ~> zip.age
    zip.out ~> Sink.head[Person]
  }

  readLine()
  sys.shutdown()
  import concurrent.duration._
  sys.awaitTermination(1.second)
}
