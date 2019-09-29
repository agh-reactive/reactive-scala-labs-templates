package EShop.lab6

import akka.actor._
import akka.event.LoggingReceive
import akka.routing._

object Worker {
  case class Work(work: String)
}

class Worker extends Actor with ActorLogging {
  import Worker._

  def receive: Receive = LoggingReceive {
    case Work(a) =>
      log.info(s"I got to work on $a")
      context.stop(self)
  }

}

object Master {
  case class WorkToDistribute(work: String)
}

class Master extends Actor with ActorLogging {
  val nbOfRoutees = 5

  val routees = Vector.fill(nbOfRoutees) {
    val r = context.actorOf(Props[Worker])
    context watch r // we subscribe for akka.actor.Terminated messages, we want to know when some worker was terminated
    ActorRefRoutee(r)
  }

  def receive: Receive = master(Router(BroadcastRoutingLogic(), routees))

  def master(router: Router): Receive = LoggingReceive {
    case Master.WorkToDistribute(w) =>
      router.route(Worker.Work(w), sender())

    case Terminated(a) => // some worker was terminated
      val r = router.removeRoutee(a)
      if (r.routees.isEmpty)
        context.system.terminate
      else
        context.become(master(r))
  }
}

object Client {
  case object Init
}

class Client extends Actor {
  import Client._

  def receive: Receive = LoggingReceive {
    case Init =>
      val master = context.actorOf(Props(classOf[Master]), "master")
      master ! Master.WorkToDistribute("some work")
  }
}

object RoutersDemo extends App {

  val system = ActorSystem("ReactiveRouters")

  val client = system.actorOf(Props(classOf[Client]), "client")

  client ! Client.Init

}

object SimpleRouterDemo extends App {

  val system = ActorSystem("ReactiveRouters")

  val workers = system.actorOf(BroadcastPool(5).props(Props[Worker]), "workersRouter")

  workers ! Worker.Work("some work")
  //workers ! Worker.Work("some work 2")

}
