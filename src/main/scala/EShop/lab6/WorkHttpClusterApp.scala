package EShop.lab6

import akka.actor.{ActorSystem, Props}
import akka.cluster.routing.{ClusterRouterPool, ClusterRouterPoolSettings}
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.util.Try

object ClusterNodeApp extends App {
  private val config = ConfigFactory.load()

  val system = ActorSystem(
    "ClusterWorkRouters",
    config
      .getConfig(Try(args(0)).getOrElse("cluster-default"))
      .withFallback(config.getConfig("cluster-default"))
  )
}

object WorkHttpClusterApp extends App {
  new WorkHttpServerInCluster().startServer("localhost", args(0).toInt)
}

class WorkHttpServerInCluster() extends HttpApp with JsonSupport {

  private val config = ConfigFactory.load()

  val system = ActorSystem(
    "ClusterWorkRouters",
    config.getConfig("cluster-default")
  )

  val workers = system.actorOf(
    ClusterRouterPool(
      RoundRobinPool(0),
      ClusterRouterPoolSettings(totalInstances = 100, maxInstancesPerNode = 3, allowLocalRoutees = false)
    ).props(Props[HttpWorker]),
    name = "clusterWorkerRouter"
  )

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
