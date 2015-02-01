package akka.stream.webinar

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.StreamTcp.OutgoingConnection
import akka.stream.scaladsl.{ Flow, StreamTcp }
import akka.util.ByteString

object AkkaHttpClient extends App {

  implicit val sys = ActorSystem()
  implicit val mat = FlowMaterializer()

  val localhost = new InetSocketAddress("127.0.0.1", 8888)

  val connection: OutgoingConnection = StreamTcp().outgoingConnection(localhost)

  val repl = Flow[ByteString]
    .transform(() ⇒ Cookbook.parseLines("\n", maximumLineBytes = 256))
    .map(text ⇒ println("Server said: " + text))
    .map(_ ⇒ readLine("> "))
    .map {
      case "q"  ⇒
        sys.shutdown(); ByteString("BYE")
      case text ⇒ ByteString(s"$text")
    }

  connection.handleWith(repl)

}
