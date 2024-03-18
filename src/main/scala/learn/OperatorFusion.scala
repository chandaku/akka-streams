package learn

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperatorFusion extends App {

 implicit val system = ActorSystem("AsyncWithOrdering")
 implicit val materialize = ActorMaterializer()
  asyncOperatorFusionWithDiffActors
  //asyncOrderingMaintainedforSourceElements


  private def asyncOrderingMaintainedforSourceElements = {
    Source(1 to 3)
      .map(element => {
        println(s"elment processing A $element");
        element
      }).async
      .map(element => {
        println(s"elment processing B $element");
        element
      }).async
      .map(element => {
        println(s"elment processing C $element");
        element
      }).async
      .runWith(Sink.ignore)
  }

  private def asyncOperatorFusionWithDiffActors = {
    val source = Source(1 to 10)
    val complexFlow = Flow[Int].map(x => {
      Thread.sleep(1000)
      x + 1
    })

    val complexFlow2 = Flow[Int].map(x => {
      Thread.sleep(1000)
      x * 10
    })
    val sink = Sink.foreach(println)
    source.via(complexFlow).async // runs on one actor
      .via(complexFlow2).async // runs on another actor
      .to(sink)
      .run()
  }

}
