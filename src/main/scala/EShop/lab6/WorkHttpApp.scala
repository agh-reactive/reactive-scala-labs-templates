package EShop.lab6

import java.net.URI

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat}

import scala.concurrent.duration._

object HttpWorker {
  case class Work(work: String)
  case class Response(result: String)
}

class HttpWorker extends Actor with ActorLogging {
  import HttpWorker._

  def receive: Receive = LoggingReceive {
    case Work(a) =>
      log.info(s"I got to work on $a")
      sender ! Response("Done")
  }

}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val workerWork     = jsonFormat1(HttpWorker.Work)
  implicit val workerResponse = jsonFormat1(HttpWorker.Response)

  //custom formatter just for example
  implicit val uriFormat = new JsonFormat[java.net.URI] {
    override def write(obj: java.net.URI): spray.json.JsValue = JsString(obj.toString)
    override def read(json: JsValue): URI = json match {
      case JsString(url) => new URI(url)
      case _             => throw new RuntimeException("Parsing exception")
    }
  }

}

object WorkHttpApp extends App {
  new WorkHttpServer().startServer("localhost", 9000)
}

class WorkHttpServer extends HttpApp with JsonSupport {

  val system  = ActorSystem("ReactiveRouters")
  val workers = system.actorOf(RoundRobinPool(5).props(Props[HttpWorker]), "workersRouter")

  implicit val timeout: Timeout = 5.seconds

  override protected def routes: Route = {
    path("work") {
      post {
        entity(as[HttpWorker.Work]) { work =>
          complete {
            (workers ? work).mapTo[HttpWorker.Response]
          }
        }
      }
    }
  }

}
