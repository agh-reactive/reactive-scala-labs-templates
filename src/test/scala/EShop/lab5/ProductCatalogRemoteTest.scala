package EShop.lab5

import EShop.lab5.ProductCatalog.GetItems
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class ProductCatalogRemoteTest extends AsyncFlatSpecLike with Matchers {

  implicit val timeout: Timeout = 3.second

  "A remote Product Catalog" should "return search results" in {
    val config = ConfigFactory.load()

    val actorSystem =
      ActorSystem[Nothing](Behaviors.empty, "ProductCatalog", config.getConfig("productcatalog").withFallback(config))
    actorSystem.systemActorOf(ProductCatalog(new SearchService()), "productcatalog")

    val anotherActorSystem =
      ActorSystem[Nothing](Behaviors.empty, "ProductCatalog")
    implicit val scheduler = anotherActorSystem.scheduler

    // wait for the cluster to form up
    Thread.sleep(3000)

    val listingFuture = anotherActorSystem.receptionist.ask(
      (ref: ActorRef[Receptionist.Listing]) => Receptionist.find(ProductCatalog.ProductCatalogServiceKey, ref)
    )

    for {
      ProductCatalog.ProductCatalogServiceKey.Listing(listing) <- listingFuture
      productCatalog = listing.head
      items <- productCatalog.ask(ref => GetItems("gerber", List("cream"), ref)).mapTo[ProductCatalog.Items]
      _ = actorSystem.terminate()
      _ = anotherActorSystem.terminate()
    } yield {
      assert(items.items.size == 10)
    }
  }
}
