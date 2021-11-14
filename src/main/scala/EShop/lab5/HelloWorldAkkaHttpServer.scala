package EShop.lab5

import EShop.lab5.HelloWorldAkkaHttpServer.Greetings
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}

import java.net.URI
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object HelloWorldAkkaHttpServer {
  case class Name(name: String)
  case class Greetings(greetings: String)
}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit lazy val nameFormat      = jsonFormat1(HelloWorldAkkaHttpServer.Name)
  implicit lazy val greetingsFormat = jsonFormat1(HelloWorldAkkaHttpServer.Greetings)

  //custom formatter just for example
  implicit lazy val uriFormat = new JsonFormat[java.net.URI] {
    override def write(obj: java.net.URI): spray.json.JsValue = JsString(obj.toString)
    override def read(json: JsValue): URI =
      json match {
        case JsString(url) => new URI(url)
        case _             => throw new RuntimeException("Parsing exception")
      }
  }

}

object HelloWorldAkkaHttpServerApp extends App {
  new HelloWorldAkkaHttpServer().start(9000)
}

/** Just to demonstrate how one can build akka-http based server with JsonSupport */
class HelloWorldAkkaHttpServer extends JsonSupport {
  implicit val system = ActorSystem[Nothing](Behaviors.empty, "HelloWorldAkkaHttp")

  def routes: Route = {
    path("greetings") {
      post {
        entity(as[HelloWorldAkkaHttpServer.Name]) { name =>
          complete {
            Future.successful(Greetings(s"Hello ${name.name}"))
          }
        }
      }
    }
  }

  def start(port: Int) = {
    val bindingFuture = Http().newServerAt("localhost", port).bind(routes)
    Await.ready(system.whenTerminated, Duration.Inf)
  }

}
