package playground

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object Playground extends App {
  /*implicit val actorSystem = ActorSystem("Playground")
  implicit val materializer = ActorMaterializer()

  Source.single("hello, Streams !")
    .to(Sink.foreach(println))
    .run()*/
  System.setProperty("Hell", "Mycom")
  System.setProperty("Hell0", "Mycom")
  val optionNames = Array("Hell0", "Hell")
  val propertiesMap = optionNames.flatMap { key => sys.props.get(key).map(key -> _)}.toMap

  // Print the properties map
  propertiesMap.foreach { case (key, value) =>
    println(s"$key = $value")
  }

}
