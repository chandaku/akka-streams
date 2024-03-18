package learn

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Concat, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, SinkShape, SourceShape}

object OpenGraphs extends App {

  implicit val system = ActorSystem("OpenGraphs")
  implicit val materialize = ActorMaterializer()

  /**
   * A Composite source that concatenates 2 sources
   - emits All the elements from the first source
   - then All the elements from the second
   */

    val firstSource = Source(1 to 10)
    val secondSource = Source(42 to 1000)

  //step1
  /*val graph = RunnableGraph.fromGraph(
    GraphDSL.create() {
      implicit builder =>

        //step2: declaring components

        //step 3: tying them together

        //step4
        ClosedShape
    }
  )*/

     val sourceGraph =  Source.fromGraph(
        GraphDSL.create() {
          implicit builder =>
            import GraphDSL.Implicits._
            //step2: declaring components
            val concat = builder.add(Concat[Int](2))
            //step 3: tying them together
            firstSource ~> concat
            secondSource ~> concat
            //step4
            SourceShape(concat.out)
        }
      )
 // sourceGraph.to(Sink.foreach(println)).run()

  val sink1 = Sink.foreach[Int](x=> println(s"meaning full sink 1 ${x}"))
  val sink2 = Sink.foreach[Int](x=> println(s"meaning full sink 2 ${x}"))

  //step1
  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() {
     implicit builder =>
       import GraphDSL.Implicits._
       val broadcast = builder.add(Broadcast[Int](2))
       broadcast ~> sink1
       broadcast ~> sink2
       SinkShape(broadcast.in)
    }
  )

  firstSource.to(sinkGraph).run()
}
