package EShop.lab5

import EShop.lab5.PaymentService.{PaymentClientError, PaymentServerError, PaymentSucceeded}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCode, StatusCodes}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.pattern.pipe

import scala.concurrent.ExecutionContext.Implicits.global

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

  override def preStart(): Unit = http.singleRequest(HttpRequest(uri = URI)).pipeTo(self)

  override def receive: Receive = {
    case HttpResponse(StatusCodes.OK, _, _, _)                  => payment ! PaymentSucceeded
    case HttpResponse(StatusCodes.InternalServerError, _, _, _) => throw new PaymentServerError
    case HttpResponse(StatusCodes.RequestTimeout, _, _, _)      => throw new PaymentServerError
    case HttpResponse(StatusCodes.ImATeapot, _, _, _)           => throw new PaymentServerError
    case HttpResponse(StatusCodes.BadRequest, _, _, _)          => throw new PaymentClientError
    case HttpResponse(StatusCodes.NotFound, _, _, _)            => throw new PaymentClientError
    case Status.Failure(exception)                              => throw exception
  }

  private def getURI: String = method match {
    case "payu"   => "http://127.0.0.1:8080"
    case "paypal" => s"http://httpbin.org/status/408"
    case "visa"   => s"http://httpbin.org/status/200"
    case _        => s"http://httpbin.org/status/404"
  }

}
