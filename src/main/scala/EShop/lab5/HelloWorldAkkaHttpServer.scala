package EShop.lab5

import java.net.URI

import EShop.lab5.HelloWorldAkkaHttpServer.Greetings
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{HttpApp, Route}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}

import scala.concurrent.Future

object HelloWorldAkkaHttpServer {
  case class Name(name: String)
  case class Greetings(greetings: String)
}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val nameFormat      = jsonFormat1(HelloWorldAkkaHttpServer.Name)
  implicit val greetingsFormat = jsonFormat1(HelloWorldAkkaHttpServer.Greetings)

  //custom formatter just for example
  implicit val uriFormat = new JsonFormat[java.net.URI] {
    override def write(obj: java.net.URI): spray.json.JsValue = JsString(obj.toString)
    override def read(json: JsValue): URI = json match {
      case JsString(url) => new URI(url)
      case _             => throw new RuntimeException("Parsing exception")
    }
  }

}

object HelloWorldAkkaHttpServerApp extends App {
  new HelloWorldAkkaHttpServer().startServer("localhost", 9000)
}

/** Just to demonstrate how one can build akka-http based server with JsonSupport */
class HelloWorldAkkaHttpServer extends HttpApp with JsonSupport {

  override protected def routes: Route = {
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

}
