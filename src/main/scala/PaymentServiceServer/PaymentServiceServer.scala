package PaymentServiceServer

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class PaymentServiceServer extends PaymentRoutes {

  implicit val system: ActorSystem[Nothing]       = ActorSystem(Behaviors.empty, "PaymentServiceSystem")
  implicit val executionContext: ExecutionContext = system.executionContext
  lazy val routes: Route                          = userRoutes

  def run() = {
    val serverBinding = Http().newServerAt("localhost", 8080).bind(routes)

    serverBinding.onComplete {
      case Success(bound) =>
        println(s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
      case Failure(e) =>
        Console.err.println(s"Server could not start!")
        e.printStackTrace()
        system.terminate()
    }

    Await.result(system.whenTerminated, Duration.Inf)
  }
}

object PaymentServiceServerApp extends App {
  val server = new PaymentServiceServer()
  server.run()
}
