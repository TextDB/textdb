package edu.uci.ics.amber.engine.architecture.deploysemantics.layer

import akka.actor.{ActorContext, ActorRef, Address, Deploy}
import akka.remote.RemoteScope
import edu.uci.ics.amber.engine.architecture.deploysemantics.deploymentfilter.DeploymentFilter
import edu.uci.ics.amber.engine.architecture.deploysemantics.deploystrategy.DeployStrategy
import edu.uci.ics.amber.engine.architecture.messaginglayer.NetworkCommunicationActor.RegisterActorRef
import edu.uci.ics.amber.engine.architecture.worker.WorkflowWorker
import edu.uci.ics.amber.engine.common.IOperatorExecutor
import edu.uci.ics.amber.engine.common.virtualidentity.{
  ActorVirtualIdentity,
  LayerIdentity,
  LinkIdentity
}
import edu.uci.ics.amber.engine.common.worker.WorkerState.Uninitialized
import edu.uci.ics.amber.engine.common.worker.{WorkerState, WorkerStatistics}
import edu.uci.ics.amber.engine.operators.OpExecConfig

import scala.collection.mutable

class WorkerLayer(
    val id: LayerIdentity,
    var metadata: Int => IOperatorExecutor,
    var numWorkers: Int,
    val deploymentFilter: DeploymentFilter,
    val deployStrategy: DeployStrategy
) extends Serializable {

  private val startDependencies = mutable.HashSet[LinkIdentity]()
  var workers: Map[ActorVirtualIdentity, WorkerInfo] = _

  def startAfter(link: LinkIdentity): Unit = {
    startDependencies.add(link)
  }

  def resolveDependency(link: LinkIdentity): Unit = {
    startDependencies.remove(link)
  }

  def hasDependency(link: LinkIdentity): Boolean = startDependencies.contains(link)

  def canStart: Boolean = startDependencies.isEmpty

  def isBuilt: Boolean = workers != null

  def identifiers: Array[ActorVirtualIdentity] = workers.values.map(_.id).toArray

  def states: Array[WorkerState] = workers.values.map(_.state).toArray

  def statistics: Array[WorkerStatistics] = workers.values.map(_.stats).toArray

  def build(
      prev: Array[(OpExecConfig, WorkerLayer)],
      all: Array[Address],
      parentNetworkCommunicationActorRef: ActorRef,
      context: ActorContext,
      workerToLayer: mutable.HashMap[ActorVirtualIdentity, WorkerLayer]
  ): Unit = {
    deployStrategy.initialize(deploymentFilter.filter(prev, all, context.self.path.address))
    workers = (0 until numWorkers).map { i =>
      val m = metadata(i)
      val workerID = ActorVirtualIdentity(s"Worker-$id-[$i]")
      val d = deployStrategy.next()
      val ref = context.actorOf(
        WorkflowWorker
          .props(workerID, m, parentNetworkCommunicationActorRef)
          .withDeploy(Deploy(scope = RemoteScope(d)))
      )
      parentNetworkCommunicationActorRef ! RegisterActorRef(workerID, ref)
      workerToLayer(workerID) = this
      workerID -> WorkerInfo(
        workerID,
        Uninitialized,
        WorkerStatistics(Uninitialized, 0, 0)
      )
    }.toMap
  }

}
