package EShop.lab5

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import java.net.URI
import java.util.zip.GZIPInputStream
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.util.Random

class SearchService() {

  private val gz = new GZIPInputStream(
    getClass.getResourceAsStream("/query_result.gz")
  )
  private[lab5] val brandItemsMap = Source
    .fromInputStream(gz)("UTF-8")
    .getLines()
    .drop(1) //skip header
    .filter(_.split(",").length >= 3)
    .map { line =>
      val values = line.split(",")
      ProductCatalog.Item(
        new URI("http://catalog.com/product/" + values(0).replaceAll("\"", "")),
        values(1).replaceAll("\"", ""),
        values(2).replaceAll("\"", ""),
        Random.nextInt(1000).toDouble,
        Random.nextInt(100)
      )
    }
    .toList
    .groupBy(_.brand.toLowerCase)

  def search(brand: String, keyWords: List[String]): List[ProductCatalog.Item] = {
    val lowerCasedKeyWords = keyWords.map(_.toLowerCase)
    brandItemsMap
      .getOrElse(brand.toLowerCase, Nil)
      .map(
        item => (lowerCasedKeyWords.count(item.name.toLowerCase.contains), item)
      )
      .sortBy(-_._1) // sort in desc order
      .take(10)
      .map(_._2)
  }
}

object ProductCatalog {
  val ProductCatalogServiceKey = ServiceKey[Query]("ProductCatalog")

  case class Item(id: URI, name: String, brand: String, price: BigDecimal, count: Int)

  sealed trait Query
  case class GetItems(brand: String, productKeyWords: List[String], sender: ActorRef[Ack]) extends Query

  sealed trait Ack
  case class Items(items: List[Item]) extends Ack

  def apply(searchService: SearchService): Behavior[Query] = Behaviors.setup { context =>
    context.system.receptionist ! Receptionist.register(ProductCatalogServiceKey, context.self)

    Behaviors.receiveMessage {
      case GetItems(brand, productKeyWords, sender) =>
        sender ! Items(searchService.search(brand, productKeyWords))
        Behaviors.same
    }
  }
}

object ProductCatalogApp extends App {

  private val config = ConfigFactory.load()

  private val productCatalogSystem = ActorSystem[Nothing](
    Behaviors.empty,
    "ProductCatalog",
    config.getConfig("productcatalog").withFallback(config)
  )

  productCatalogSystem.systemActorOf(
    ProductCatalog(new SearchService()),
    "productcatalog"
  )

  Await.result(productCatalogSystem.whenTerminated, Duration.Inf)
}
