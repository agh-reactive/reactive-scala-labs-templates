package EShop.lab6

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{as, complete, entity, path, post}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.Try

/**
 * Basically a [[HttpWorker]] that registers itself with the receptionist
 *
 * @see https://doc.akka.io/docs/akka/current/typed/actor-discovery.html#receptionist
 */
object RegisteredHttpWorker {
  val HttpWorkerKey: ServiceKey[HttpWorker.Command] = ServiceKey("HttpWorker")

  def apply(): Behavior[HttpWorker.Command] =
    Behaviors.setup { context =>
      context.system.receptionist ! Receptionist.Register(HttpWorkerKey, context.self)

      Behaviors.receive((context, msg) =>
        msg match {
          case HttpWorker.Work(work, replyTo) =>
            context.log.info(s"I got to work on $work")
            replyTo ! HttpWorker.WorkerResponse("Done")
            Behaviors.same
        }
      )
    }
}

/**
 * Spawns an actor system that will connect with the cluster and spawn `instancesPerNode` workers
 */
class HttpWorkersNode {
  private val instancesPerNode = 3
  private val config           = ConfigFactory.load()

  val system = ActorSystem[Nothing](
    Behaviors.empty,
    "ClusterWorkRouters",
    config
  )

  for (i <- 0 to instancesPerNode) system.systemActorOf(RegisteredHttpWorker(), s"worker$i")

  def terminate(): Unit =
    system.terminate()
}

/**
 * Spawns a seed node
 */
object ClusterNodeApp extends App {
  private val config = ConfigFactory.load()

  val system = ActorSystem[Nothing](
    Behaviors.empty,
    "ClusterWorkRouters",
    config
      .getConfig(Try(args(0)).getOrElse("seed-node1"))
      .withFallback(config)
  )

  Await.ready(system.whenTerminated, Duration.Inf)
}

object WorkHttpClusterApp extends App {
  val workHttpServerInCluster = new WorkHttpServerInCluster()
  workHttpServerInCluster.run(args(0).toInt)
}

/**
 * The server that distributes all of the requests to the workers registered in the cluster via the group router.
 * Will spawn `httpWorkersNodeCount` [[HttpWorkersNode]] instances that will each spawn `instancesPerNode`
 * [[RegisteredHttpWorker]] instances giving us `httpWorkersNodeCount` * `instancesPerNode` workers in total.
 *
 * @see https://doc.akka.io/docs/akka/current/typed/routers.html#group-router
 */
class WorkHttpServerInCluster() extends JsonSupport {
  private val config               = ConfigFactory.load()
  private val httpWorkersNodeCount = 10

  implicit val system = ActorSystem[Nothing](
    Behaviors.empty,
    "ClusterWorkRouters",
    config.getConfig("cluster-default")
  )

  implicit val scheduler        = system.scheduler
  implicit val executionContext = system.executionContext

  val workersNodes = for (_ <- 0 to httpWorkersNodeCount) yield new HttpWorkersNode()

  val workers = system.systemActorOf(Routers.group(RegisteredHttpWorker.HttpWorkerKey), "clusterWorkerRouter")

  implicit val timeout: Timeout = 5.seconds

  def routes: Route =
    path("work") {
      post {
        entity(as[WorkDTO]) { workDto =>
          complete {
            workers.ask(replyTo => HttpWorker.Work(workDto.work, replyTo))
          }
        }
      }
    }

  def run(port: Int): Unit = {
    val bindingFuture = Http().newServerAt("localhost", port).bind(routes)
    println(s"Server now online.\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete { _ =>
        system.terminate()
        workersNodes.foreach(_.terminate())
      } // and shutdown when done
  }
}
