package EShop.lab5

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object ProductCatalogServerApp extends App {
  private val config  = ConfigFactory.load()
  val httpActorSystem = ActorSystem("server", config.getConfig("server").withFallback(config))

  private val productCatalogSystem = ActorSystem(
    "ProductCatalog",
    config.getConfig("productcatalog").withFallback(config)
  )

  productCatalogSystem.actorOf(
    ProductCatalog.props(new SearchService()),
    "productcatalog"
  )

  val server = new ProductCatalogServer(httpActorSystem)
  server.startServer("localhost", 8888, httpActorSystem)
}