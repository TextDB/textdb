package edu.uci.ics.amber.engine.architecture.linksemantics

import edu.uci.ics.amber.engine.architecture.deploysemantics.layer.ActorLayer
import edu.uci.ics.amber.engine.architecture.sendsemantics.datatransferpolicy.{
  HashBasedShufflePolicy,
  RoundRobinPolicy
}
import edu.uci.ics.amber.engine.common.AdvancedMessageSending
import edu.uci.ics.amber.engine.common.ambermessage.WorkerMessage.{
  UpdateInputLinking,
  UpdateOutputLinking
}
import edu.uci.ics.amber.engine.common.tuple.ITuple
import akka.event.LoggingAdapter
import akka.util.Timeout

import scala.concurrent.ExecutionContext

class HashBasedShuffle(
    from: ActorLayer,
    to: ActorLayer,
    batchSize: Int,
    hashFunc: ITuple => Int,
    inputNum: Int
) extends LinkStrategy(from, to, batchSize, inputNum) {
  override def link()(implicit
      timeout: Timeout,
      ec: ExecutionContext
  ): Unit = {
    assert(from.isBuilt && to.isBuilt)
    from.layer.foreach(x =>
      AdvancedMessageSending.blockingAskWithRetry(
        x,
        UpdateOutputLinking(
          new HashBasedShufflePolicy(batchSize, hashFunc),
          tag,
          to.identifiers
        ),
        10
      )
    )
  }
}
