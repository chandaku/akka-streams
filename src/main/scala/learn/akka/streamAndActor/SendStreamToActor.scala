package learn.akka.streamAndActor

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import org.scalatest.time.Seconds

object SendStreamToActor extends App {

  implicit val system = ActorSystem("SendStreamToActor")
  implicit val materialize = ActorMaterializer()

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s: String =>
        log.info(s"message receieved is String $s")
        sender() ! s"$s$s"
      case n: Int =>
        log.info(s"receieved numeric $n")
        sender() ! (n*n)
      case _ =>
    }
  }
  val numberSource = Source(1 to 10)
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")
  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)
  val sink = Sink.foreach[Int](println)
  val actorBasedFlow = Flow[Int].ask[Int](parallelism = 4)(simpleActor)

  //numberSource.via(actorBasedFlow).to(Sink.foreach[Int](println)).run()
  Source(1 to 10).ask[Int](parallelism = 4)(simpleActor).to(sink).run()
}
