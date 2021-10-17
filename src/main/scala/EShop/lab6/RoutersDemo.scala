package EShop.lab6

import akka.actor.typed._
import akka.actor.typed.scaladsl.{Behaviors, Routers}

/**
 * Simple worker that logs the content of the received work and stops
 */
object Worker {
  case class Work(work: String)

  def apply(): Behavior[Work] =
    Behaviors.receive[Work](
      (context, msg) =>
        msg match {
          case Work(work) =>
            context.log.info(s"I got to work on $work")
            Behaviors.stopped
      }
    )
}

/**
 * Master that spawns `nbOfRoutees` local workers via pool router and distributes the work between them
 *
 * @see https://doc.akka.io/docs/akka/current/typed/routers.html#pool-router
 */
object Master {
  case class WorkToDistribute(work: String)

  val nbOfRoutees = 5

  def apply(): Behavior[WorkToDistribute] =
    Behaviors.setup { context =>
      val pool   = Routers.pool(poolSize = nbOfRoutees)(Worker())
      val router = context.spawn(pool, "worker-pool")
      context.watch(router)

      Behaviors
        .receiveMessage[WorkToDistribute] {
          case WorkToDistribute(work) =>
            router ! Worker.Work(work)
            Behaviors.same
        }
        .receiveSignal {
          case (context, Terminated(router)) =>
            context.system.terminate()
            Behaviors.stopped
        }
    }
}

/**
 * Client that demonstrates the [[Master]] behaviour
 */
object Client {
  case object Init

  def apply(): Behavior[Init.type] =
    Behaviors.receive(
      (context, msg) =>
        msg match {
          case Init =>
            val master = context.spawn(Master(), "master")
            master ! Master.WorkToDistribute("some work")
            Behaviors.same
      }
    )
}

/**
 * Runs the Client actor that demonstrates the behaviour of local pool routers
 */
object RoutersDemo extends App {
  val system = ActorSystem(Behaviors.empty, "ReactiveRouters")
  val client = system.systemActorOf(Client(), "client")
  client ! Client.Init
}

/**
 * Simple app that demonstrates the behaviour of pool router's broadcast mechanism
 */
object SimpleRouterDemo extends App {
  val system = ActorSystem(Behaviors.empty, "ReactiveRouters")

  val pool    = Routers.pool(poolSize = 5)(Worker()).withBroadcastPredicate(_ => true)
  val workers = system.systemActorOf(pool, "broadcast-workers")

  workers ! Worker.Work("some work")
  //  workers ! Worker.Work("some work 2") // won't work since all of the actors inside the pool have received previous message and have stopped
}
