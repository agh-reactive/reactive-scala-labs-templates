package EShop.lab5
import java.net.URI
import java.util.zip.GZIPInputStream

import akka.actor.{Actor, ActorSystem, Props}
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
    .fromInputStream(gz)
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
  case class Item(id: URI, name: String, brand: String, price: BigDecimal, count: Int)

  sealed trait Query
  case class GetItems(brand: String, productKeyWords: List[String]) extends Query

  sealed trait Ack
  case class Items(items: List[Item]) extends Ack

  def props(searchService: SearchService): Props =
    Props(new ProductCatalog(searchService))
}

class ProductCatalog(searchService: SearchService) extends Actor {

  import ProductCatalog._

  override def receive: Receive = {
    case GetItems(brand, productKeyWords) =>
      sender() ! Items(searchService.search(brand, productKeyWords))
  }
}

object ProductCatalogApp extends App {

  private val config = ConfigFactory.load()

  private val productCatalogSystem = ActorSystem(
    "ProductCatalog",
    config.getConfig("productcatalog").withFallback(config)
  )

  productCatalogSystem.actorOf(
    ProductCatalog.props(new SearchService()),
    "productcatalog"
  )

  Await.result(productCatalogSystem.whenTerminated, Duration.Inf)
}
