package PaymentServiceServer

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.Random

trait PaymentRoutes {

  implicit def system: ActorSystem

  implicit lazy val timeout = Timeout(5.seconds)

  var counter = 0

  lazy val userRoutes: Route =
    get {
      complete(getResponse)
    }

  private def getResponse = {
    if (counter < 2) {
      counter += 1
      ImATeapot
    } else if (counter == 2) {
      counter += 1
      OK
    } else {
      Random.nextInt(6) match {
        case 0 | 1 | 2 => OK
        case 3         => InternalServerError
        case 4         => RequestTimeout
        case _         => ImATeapot
      }
    }

  }
}
