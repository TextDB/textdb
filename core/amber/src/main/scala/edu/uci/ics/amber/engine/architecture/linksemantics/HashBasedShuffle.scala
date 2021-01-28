package edu.uci.ics.amber.engine.architecture.linksemantics

import edu.uci.ics.amber.engine.architecture.deploysemantics.layer.WorkerLayer
import edu.uci.ics.amber.engine.architecture.sendsemantics.datatransferpolicy.{DataSendingPolicy, HashBasedShufflePolicy, RoundRobinPolicy}
import edu.uci.ics.amber.engine.common.tuple.ITuple
import akka.event.LoggingAdapter
import akka.util.Timeout
import edu.uci.ics.amber.engine.common.ambertag.neo.VirtualIdentity.ActorVirtualIdentity

import scala.concurrent.ExecutionContext

class HashBasedShuffle(
    from: WorkerLayer,
    to: WorkerLayer,
    batchSize: Int,
    hashFunc: ITuple => Int,
    inputNum: Int
) extends LinkStrategy(from, to, batchSize, inputNum) {
  override def getPolicies(): Iterable[(ActorVirtualIdentity, DataSendingPolicy, Seq[ActorVirtualIdentity])] = {
    assert(from.isBuilt && to.isBuilt)
    from.identifiers.map(x =>

      ( x, new HashBasedShufflePolicy(tag, batchSize, hashFunc, to.identifiers), to.identifiers.toSeq)
    )
  }
}
