package EShop.lab5

import EShop.lab5.ProductCatalog.{GetItems, Items}
import akka.actor.{Actor, ActorSystem}
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ProductCatalogServer(actorSystem: ActorSystem) extends HttpApp with JsonSupport{
  private implicit val timeout: Timeout     = Timeout(5.seconds)
  override protected def routes: Route = {
    path("get-item") {
      post {
        entity(as[GetItems]) { query =>
          val productCatalog = actorSystem.actorSelection("akka.tcp://ProductCatalog@127.0.0.1:2553/user/productcatalog")
          complete {
            (for {
              actor <- productCatalog.resolveOne()
              response <- actor ? query
            } yield response).mapTo[Items]
          }
        }
      }
    }
  }
}
