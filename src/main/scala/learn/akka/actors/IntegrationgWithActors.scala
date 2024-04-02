package learn.akka.actors

import akka.actor.Props
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration._



object IntegrationgWithActors extends App {

  import akka.actor.{Actor, ActorLogging, ActorSystem}
  implicit val system = ActorSystem("IntegrationgWithActors")
  implicit val materialize = ActorMaterializer()

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s:String =>
        log.info(s"Just receieved a string: $s")
        sender() ! s"$s$s"
      case n:Int =>
        log.info(s"Just receieved a number: $n")
        sender() ! 2*n
      case _ =>
    }
  }

  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  val numberSource = Source(1 to 10)

  implicit val timeout = Timeout(2 seconds)
  val actorBasedFlow = Flow[Int].ask[Int](4)(simpleActor)

  //numberSource.via(actorBasedFlow).to(Sink.foreach(println)).run()

  //numberSource.ask[Int](4)(simpleActor).to(Sink.ignore).run()

  /**
   * Actor as a source
   */

  val actorPoweredSource = Source.actorRef[Int](bufferSize = 10, overflowStrategy = OverflowStrategy.dropHead)

  val materializedValue = actorPoweredSource.to(Sink.foreach[Int](x => println(s"Actor powered flow got $x"))).run()

  materializedValue ! 10

  // terminiating the stream

  //materializedActorRef ! akka.actor.Status.Success("Completed")

  /*
  Actor as a desitnation/sink
  - an init message
  - an ack message to confirm the reception
  - a complete message
  - a function to generate a messae in case the stream throws an exception
  */

  /*case object StreamInit
  csae object StreamAck
  csae object StreamComplete
  csae object StreamFail(ex: Throwabled)

  class DestinationActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StreamInit =>
        log.info("Stream initialized")
        sender() ! StreamAck
      case StreamComplete =>
        log.info("Stream cinoketed")
        context.stop(self)
      case StreamFail(ex) =>
        log.warning("Stream failed")
      case message =>
        log.info("Message has come to its final resting point.")
        sender() ! StreamAck
    }
  }

  val destinitionActor = system.actorOf(Props[Destinition], "destitionationActor")*/
}
