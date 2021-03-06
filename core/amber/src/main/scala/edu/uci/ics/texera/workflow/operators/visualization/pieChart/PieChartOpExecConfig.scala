package edu.uci.ics.texera.workflow.operators.visualization.pieChart

import edu.uci.ics.amber.engine.architecture.breakpoint.globalbreakpoint.GlobalBreakpoint
import edu.uci.ics.amber.engine.architecture.deploysemantics.deploymentfilter.{
  FollowPrevious,
  UseAll
}
import edu.uci.ics.amber.engine.architecture.deploysemantics.deploystrategy.RoundRobinDeployment
import edu.uci.ics.amber.engine.architecture.deploysemantics.layer.WorkerLayer
import edu.uci.ics.amber.engine.architecture.linksemantics.HashBasedShuffle
import edu.uci.ics.amber.engine.common.Constants
import edu.uci.ics.amber.engine.common.virtualidentity.util.makeLayer
import edu.uci.ics.amber.engine.common.virtualidentity.{
  ActorVirtualIdentity,
  LayerIdentity,
  OperatorIdentity
}
import edu.uci.ics.amber.engine.operators.OpExecConfig
import edu.uci.ics.texera.workflow.common.tuple.schema.OperatorSchemaInfo

class PieChartOpExecConfig(
    tag: OperatorIdentity,
    val numWorkers: Int,
    val nameColumn: String,
    val dataColumn: String,
    val pruneRatio: Double,
    operatorSchemaInfo: OperatorSchemaInfo
) extends OpExecConfig(tag) {

  override lazy val topology: Topology = {
    val partialLayer = new WorkerLayer(
      makeLayer(tag, "localPieChartProcessor"),
      _ => new PieChartOpPartialExec(nameColumn, dataColumn),
      numWorkers,
      UseAll(),
      RoundRobinDeployment()
    )
    val finalLayer = new WorkerLayer(
      makeLayer(tag, "globalPieChartProcessor"),
      _ => new PieChartOpFinalExec(pruneRatio),
      1,
      FollowPrevious(),
      RoundRobinDeployment()
    )
    new Topology(
      Array(
        partialLayer,
        finalLayer
      ),
      Array(
        new HashBasedShuffle(
          partialLayer,
          finalLayer,
          Constants.defaultBatchSize,
          getPartitionColumnIndices(partialLayer.id)
        )
      )
    )
  }
  override def getPartitionColumnIndices(layer: LayerIdentity): Array[Int] = {
    operatorSchemaInfo.inputSchemas(0).getAttributes.toArray.indices.toArray
  }

  override def assignBreakpoint(
      breakpoint: GlobalBreakpoint[_]
  ): Array[ActorVirtualIdentity] = {
    topology.layers(0).identifiers
  }

}
