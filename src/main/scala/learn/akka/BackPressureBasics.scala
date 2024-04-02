package learn.akka

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

object BackPressureBasics extends App {
  implicit val system = ActorSystem("BackPressureBasics")
  implicit val materialize = ActorMaterializer()

  val fastSource = Source(1 to 1000)
  val simpleFlow = Flow[Int].map(x => {
    println(s"incoming element ${x}")
    x
  })

  val slowSink = Sink.foreach[Int] { x =>
    Thread.sleep(1000)
    println(s"Sink ${x}")
  }


 /* Source(1 to 1000).async
    .via(simpleFlow).async
    .to(slowSink)
    .run()*/

  /*
  reactions to backpressure (in order):
  - try to slow down if possible
  - buffer elements until there's more demand
  - drop down elements from the buffer if it overflows
  -tear down/kill the whole stream( failure)
   */

  val bufferedFlow = simpleFlow.buffer(10, OverflowStrategy.dropTail)
  /*fastSource.async
    .via(bufferedFlow).async
    .to(slowSink)
    .run()*/
  import scala.concurrent.duration._
  fastSource.throttle(1000, 1 seconds)
    .runWith(Sink.foreach(println))
}
