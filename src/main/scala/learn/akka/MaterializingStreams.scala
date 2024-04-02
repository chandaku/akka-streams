package learn.akka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

object MaterializingStreams extends App {
 implicit val system = ActorSystem("MaterializingStreams")
 implicit val materializer = ActorMaterializer()
 val source = Source(1 to 10)
 val flow = Flow[Int].map(x=>x+1)
 val sink = Sink.foreach(println)
 //val graph = source.runWith(sink)
 val graph = source.via(flow).to(sink)

 //graph will NotUsed materilized
 graph.run()

 val sinkReduce = Sink.reduce[Int]((x,y) => x+y)

 val sumFuture = source.runWith(sinkReduce)

 import system.dispatcher

 /*sumFuture.onComplete {
  case Success(value) => println(s"The sum of all the values is $value")
  case Failure(exception) => println(s"Exception caused $exception")
 }*/

 val rightMat = source.viaMat(flow)(Keep.right).toMat(sinkReduce)(Keep.right).run()

 rightMat.onComplete {
  case Success(value) => println(s"The sum of all the values is $value")
  case Failure(exception) => println(s"Exception caused $exception")
 }

 /**
  * -- return the last element ofu of a source usin Sink.lat
  * - compute total word count out of a stream of sentences
  * try to use = map, fold ,reduce
  *
  */



}
