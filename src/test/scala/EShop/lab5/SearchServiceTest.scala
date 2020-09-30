package EShop.lab5
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SearchServiceTest extends AnyFlatSpec with Matchers {

  "A Search Service" should "load the file from resources" in {
    val searchService = new SearchService()
    searchService.brandItemsMap.size shouldBe 10977
  }
}
