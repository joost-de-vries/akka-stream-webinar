package akka.stream.webinar

import java.net.InetSocketAddress
import scala.io.StdIn._

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{ Concat, Flow, Source, StreamTcp, UndefinedSink, UndefinedSource }
import akka.util.ByteString

object AkkaHttpServer extends App {

  implicit val sys = ActorSystem()
  implicit val mat = FlowMaterializer()

  val localhost = new InetSocketAddress("127.0.0.1", 8888)

  val binding = StreamTcp().bind(localhost)

  binding.connections foreach { connection ⇒

    val serverLogic = Flow() { implicit b ⇒
      import akka.stream.scaladsl.FlowGraphImplicits._

      // to be filled in by StreamTCP
      val in = UndefinedSource[ByteString]
      val out = UndefinedSink[ByteString]

      val welcomeMsg =
        s"""|Welcome to: ${connection.localAddress}!
                |You are: ${connection.remoteAddress}!""".stripMargin

      val welcome = Source.single(ByteString(welcomeMsg))
      val echo = Flow[ByteString]
        .transform(() ⇒ Cookbook.parseLines("\n", maximumLineBytes = 256))
        .map(_ ++ "!!!")
        .map(ByteString(_))

      val concat = Concat[ByteString]
      // first we emit the welcome message,
      welcome ~> concat.first
      // then we continue using the echo-logic Flow
      in ~> echo ~> concat.second

      concat.out ~> out
      (in, out)
    }

    connection.handleWith(serverLogic)
  }

}
