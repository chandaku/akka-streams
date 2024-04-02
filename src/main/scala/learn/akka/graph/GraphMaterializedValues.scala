package learn.akka.graph

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, SinkShape}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GraphMaterializedValues extends App {

  implicit val system = ActorSystem("GraphMaterializedValues")
  implicit val materialize = ActorMaterializer()

  val wordSource = Source(List("Akka", "is", "awesome", "rock", "the", "jvm"))

  val printer = Sink.foreach[String](println)
  val counter = Sink.fold[Int, String](0)((count, _) => count + 1)

  /*
  A composite component (sink)
  - prints out all strings which ar elowercase
  - counts the strings that are shourt(<5 chars)
   */

  /* val complexWordSink = Sink.fromGraph(
     GraphDSL.create(counter) {
       implicit builder =>
         counterShape =>
           import GraphDSL.Implicits._
           val broadcast = builder.add(Broadcast[String](2))
           val lowercaseFilter = builder.add(Flow[String].filter(word => word == word.toLowerCase))
           val shortStringFilter = builder.add(Flow[String].filter(_.length < 5))

           broadcast ~> lowercaseFilter ~> printer
           broadcast ~> shortStringFilter ~> counterShape
           SinkShape(broadcast.in)
     }
   )*/

  val complexWordSink = Sink.fromGraph(
    GraphDSL.create(printer, counter)((printerMatValue, counterMatValue) => counterMatValue) {
      implicit builder =>
        (printerShape, counterShape) =>
          import GraphDSL.Implicits._
          val broadcast = builder.add(Broadcast[String](2))
          val lowercaseFilter = builder.add(Flow[String].filter(word => word == word.toLowerCase))
          val shortStringFilter = builder.add(Flow[String].filter(_.length < 5))

          broadcast ~> lowercaseFilter ~> printerShape
          broadcast ~> shortStringFilter ~> counterShape
          SinkShape(broadcast.in)
    }
  )

  val shortStringCountFuture = wordSource.toMat(complexWordSink)(Keep.right).run()

  import system.dispatcher

  shortStringCountFuture.onComplete {
    case Success(value) => println(s"count value $value")
    case Failure(exception) => println(s"count value failed $exception")
  }

  //wordSource.to(complexWordSink).run()

  /**
   * Excercise
   */

  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val counterSink = Sink.fold[Int, B](0)((count, elment) => count + 1)

    Flow.fromGraph(
      GraphDSL.create(counterSink) {
        implicit builder =>
          counterSinkShape => {
            import GraphDSL.Implicits._
            val broadcast = builder.add(Broadcast[B](2))
            val originalFlowShape = builder.add(flow)
            originalFlowShape ~> broadcast ~> counterSinkShape
            FlowShape(originalFlowShape.in, broadcast.out(1))
          }
      }
    )
  }

  val simpleSource = Source(1 to 42)

  val simpleFlow = Flow[Int].map(x => x)

  val simpleSink = Sink.ignore

  val enhancedFlowCountFuture = simpleSource.viaMat(enhanceFlow(simpleFlow))(Keep.right).toMat(simpleSink)(Keep.left).run()

  enhancedFlowCountFuture.onComplete {
    case Success(count) => println(s"$count elements went through the enhanced flow")
    case Failure(exception) => println(s"something wrong has happened $exception")
  }

}
