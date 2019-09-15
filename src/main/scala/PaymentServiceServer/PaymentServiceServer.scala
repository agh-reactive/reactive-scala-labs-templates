package PaymentServiceServer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class PaymentServiceServer extends PaymentRoutes {

  implicit val system: ActorSystem                = ActorSystem("PaymentServiceSystem")
  implicit val materializer: ActorMaterializer    = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
  lazy val routes: Route                          = userRoutes

  def run() = {
    val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandle(routes, "localhost", 8080)

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
