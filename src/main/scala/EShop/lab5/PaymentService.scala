package EShop.lab5

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

object PaymentService {

  case object PaymentSucceeded // http status 200
  class PaymentClientError extends Exception // http statuses 400, 404
  class PaymentServerError extends Exception // http statuses 500, 408, 418

  def props(method: String, payment: ActorRef) = Props(new PaymentService(method, payment))

}

class PaymentService(method: String, payment: ActorRef) extends Actor with ActorLogging {

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  private val http = Http(context.system)
  private val URI  = getURI

  override def preStart(): Unit = ??? //create http request (use http and uri)

  override def receive: Receive = ???

  private def getURI: String = method match {
    case "payu"   => "http://127.0.0.1:8080"
    case "paypal" => s"http://httpbin.org/status/408"
    case "visa"   => s"http://httpbin.org/status/200"
    case _        => s"http://httpbin.org/status/404"
  }

}
