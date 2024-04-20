package learn.akka.graph

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}
import akka.stream.{ActorMaterializer, ClosedShape}

object GraphBasics extends App {

  implicit val system = ActorSystem("GraphBasics")
  implicit val materialize = ActorMaterializer()

  val input = Source(1 to 1000)

  val incrementer = Flow[Int].map(x => x + 1)

  val multiplier = Flow[Int].map(x => x * 10)

  val sink = Sink.foreach[(Int, Int)](println)

  //step 1 setting up the fundamentals for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>  //builder mutable data structure
      import GraphDSL.Implicits._ // brings some nice operators into scope
      val broadcast = builder.add(Broadcast[Int](2))
      val zip = builder.add(Zip[Int, Int])

      input ~> broadcast
      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1
      zip.out ~> sink

      ClosedShape // Freeze the builder shape

    }
  )

 // graph.run()
  val sink1 = Sink.foreach[Int](println)
  val sink2 = Sink.foreach[Int](println)
  val graphToMultipleSink = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._ // brings some nice operators into scope
      val broadcast = builder.add(Broadcast[Int](2))

      input ~> broadcast ~> sink1
               broadcast ~> sink2

      ClosedShape
    }
  )

  //graphToMultipleSink.run()

  /**
   * balance
   */

  import scala.concurrent.duration._

  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)

  /*  val balanceSink1 = Sink.foreach[Int](x=> println(s"Sink 1 ${x}"))
  val balanceSink2 = Sink.foreach[Int](x=> println(s"Sink 2 ${x}"))*/

  val balanceSink1 = Sink.fold[Int, Int](0)((count, element) => {
    println(s"Sink 1 number of element count $count")
    count + 1
  })
  val balanceSink2 = Sink.fold[Int, Int](0)((count, element) => {
    println(s"Sink 2 number of element count $count")
    count + 1
  })

  val balancedGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._ // brings some nice operators into scope

      val merge = builder.add(Merge[Int](2))

      val balance = builder.add(Balance[Int](2))

      fastSource ~> merge ~> balance ~> balanceSink1
      slowSource ~> merge;
      balance ~> balanceSink2

      ClosedShape
    }
  )
  balancedGraph.run()

}
