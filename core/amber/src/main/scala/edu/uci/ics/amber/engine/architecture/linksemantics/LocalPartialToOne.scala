package edu.uci.ics.amber.engine.architecture.linksemantics

import edu.uci.ics.amber.engine.architecture.deploysemantics.layer.ActorLayer
import edu.uci.ics.amber.engine.architecture.sendsemantics.datatransferpolicy.{
  OneToOnePolicy,
  RoundRobinPolicy
}
import edu.uci.ics.amber.engine.common.AdvancedMessageSending
import edu.uci.ics.amber.engine.common.ambermessage.WorkerMessage.{
  UpdateInputLinking,
  UpdateOutputLinking
}
import akka.event.LoggingAdapter
import akka.util.Timeout

import scala.concurrent.ExecutionContext

class LocalPartialToOne(from: ActorLayer, to: ActorLayer, batchSize: Int, inputNum: Int)
    extends LinkStrategy(from, to, batchSize, inputNum) {
  override def link()(implicit
      timeout: Timeout,
      ec: ExecutionContext
  ): Unit = {
    assert(from.isBuilt && to.isBuilt)
    val froms = from.layer.groupBy(actor => actor.path.address.hostPort)
    val tos = to.layer.groupBy(actor => actor.path.address.hostPort)
    val actorToIdentifier = (from.layer.indices.map(x =>
      from.layer(x) -> from.identifiers(x)
    ) ++ to.layer.indices.map(x => to.layer(x) -> to.identifiers(x))).toMap
    assert(froms.keySet == tos.keySet && tos.forall(x => x._2.length == 1))
    froms.foreach(x => {
      for (i <- x._2.indices) {
        AdvancedMessageSending.blockingAskWithRetry(
          x._2(i),
          UpdateOutputLinking(
            new OneToOnePolicy(batchSize),
            tag,
            Array(actorToIdentifier(tos(x._1).head))
          ),
          10
        )
      }
    })
  }
}
