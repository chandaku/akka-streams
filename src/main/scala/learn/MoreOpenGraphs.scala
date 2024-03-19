package learn

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, SinkShape, SourceShape, UniformFanInShape}

import java.util.Date

object MoreOpenGraphs extends App {

  implicit val system = ActorSystem("MoreOpenGraphs")
  implicit val materialize = ActorMaterializer()

  /*
  Example Max 3 operator
  - 3 inputs of type int
  - the max of the 3
   */

  val max3StaticGraph = GraphDSL.create() {
    implicit builder =>
      import GraphDSL.Implicits._

      //steps - define Aug SHAPES
      val max1 = builder.add(ZipWith[Int, Int, Int]((a,b)=> Math.max(a, b)))
      val max2 = builder.add(ZipWith[Int, Int, Int]((a,b)=> Math.max(a, b)))
      val max3 = builder.add(ZipWith[Int, Int, Int]((a,b)=> Math.max(a, b)))

      max1.out ~> max3.in0
      max2.out ~> max3.in1

      UniformFanInShape(max3.out, max1.in0, max1.in1, max2.in0, max2.in1)
  }

  val source1 = Source(1 to 10)
  val source2 = Source(1 to 10).map(_ => 5)
  val source3 = Source((1 to 10).reverse)
  val source4 = Source(1 to 10).map(_ => 7)

  val maxSink = Sink.foreach[Int](x=> println(s"Max is $x"))

  /* RunnableGraph.fromGraph(
    GraphDSL.create() {
      implicit builder =>
      import GraphDSL.Implicits._

      ClosedShape
    }
  )*/

  val max3RunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val max3Shape = builder.add(max3StaticGraph)
      source1 ~> max3Shape.in(0)
      source2 ~> max3Shape.in(1)
      source3 ~> max3Shape.in(2)
      source4 ~> max3Shape.in(3)
      max3Shape ~> maxSink
      ClosedShape
    }
  )
  //max3RunnableGraph.run()


  /*
  Non-uniform fanout shape
  Processing bank transactions
  Txns suspicious if amout > 1000

  Streams component for txns
  - output1 : let the transaciton go through
  - output2: suspicious txns ids
  */

  case class Transaction(id: String, source: String, recipient: String, amount:Int, date: Date)

  val tansactionsSource = Source(List(
    Transaction("523423423", "Chandan", "Poonam", 100, new Date),
    Transaction("523423233", "Rahul", "Poonam", 100000, new Date),
    Transaction("5234432423", "Heenal", "Poonam", 5000, new Date)
  ))

  val bankProcessor = Sink.foreach[Transaction](println)
  val suspiciousAnalysisService = Sink.foreach[String](txnId=>println(s"Suspicious Txn Id $txnId"))

  val suspicouosTxnStaticGraph = GraphDSL.create() {
    implicit builder =>
      import GraphDSL.Implicits._
      //Step2
      val broadcast = builder.add(Broadcast[Transaction](2))
      val suspiciousTransactionFltr = builder.add(Flow[Transaction].filter(txn=> txn.amount > 10000))
      val txnIdExtractor = builder.add(Flow[Transaction].map[String](txn => txn.id))
      //step3
      broadcast.out(0) ~> suspiciousTransactionFltr ~> txnIdExtractor
      new FanOutShape2(broadcast.in, broadcast.out(1), txnIdExtractor.out)
  }

  val suspiciousTransactionRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._
        val staticTransactionShape = builder.add(suspicouosTxnStaticGraph)
        tansactionsSource ~> staticTransactionShape.in
        staticTransactionShape.out0 ~> bankProcessor
        staticTransactionShape.out1 ~> suspiciousAnalysisService

        ClosedShape
    }
  )
      suspiciousTransactionRunnableGraph.run()


}
