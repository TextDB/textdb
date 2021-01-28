package edu.uci.ics.amber.engine.common.ambermessage

import edu.uci.ics.amber.engine.operators.OpExecConfig
import edu.uci.ics.amber.engine.architecture.breakpoint.FaultedTuple
import edu.uci.ics.amber.engine.architecture.breakpoint.localbreakpoint.LocalBreakpoint
import edu.uci.ics.amber.engine.common.tuple.ITuple
import akka.actor.ActorRef
import edu.uci.ics.amber.engine.common.ambermessage.neo.ControlPayload
import edu.uci.ics.amber.engine.common.ambertag.OperatorIdentifier
import edu.uci.ics.amber.error.WorkflowRuntimeError

object ControlMessage {

  final case class Start()

  final case class Pause()

  final case class ModifyLogic(newMetadata: OpExecConfig)

  final case class Resume()

  final case class QueryState()

  final case class QueryStatistics()

  final case class CollectSinkResults()

  final case class LocalBreakpointTriggered() extends ControlPayload

  final case class RequireAck(msg: Any)

  final case class Ack()

  final case class AckWithInformation(info: Any)

  final case class AckWithSequenceNumber(sequenceNumber: Long)

  final case class AckOfEndSending()

  final case class StashOutput()

  final case class ReleaseOutput()

  final case class SkipTuple(faultedTuple: FaultedTuple)

  final case class SkipTupleGivenWorkerRef(actorPath: String, faultedTuple: FaultedTuple)

  final case class ModifyTuple(faultedTuple: FaultedTuple)

  final case class ResumeTuple(faultedTuple: FaultedTuple)

  final case class KillAndRecover()

  final case class LogErrorToFrontEnd(err: WorkflowRuntimeError)

  final case class DetectSkew() // join-skew research related

  final case class DetectSkewTemp(opId: OperatorIdentifier) // join-skew research related
}
