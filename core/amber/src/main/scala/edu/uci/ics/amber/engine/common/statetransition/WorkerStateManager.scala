package edu.uci.ics.amber.engine.common.statetransition

import edu.uci.ics.amber.engine.common.statetransition.StateManager.IntermediateState
import edu.uci.ics.amber.engine.common.statetransition.WorkerStateManager._

object WorkerStateManager {
  sealed abstract class WorkerState extends Product with Serializable
  case object UnInitialized extends WorkerState
  case object Ready extends WorkerState
  case object Running extends WorkerState
  case object Paused extends WorkerState
  case object Pausing extends WorkerState with IntermediateState
  case object Completed extends WorkerState
  case object Recovering extends WorkerState with IntermediateState

}

class WorkerStateManager
    extends StateManager[WorkerState](
      Map(
        UnInitialized -> Set(Ready, Recovering),
        Ready -> Set(Pausing, Running, Recovering),
        Running -> Set(Pausing, Completed, Recovering),
        Pausing -> Set(Paused, Recovering),
        Paused -> Set(Running, Recovering),
        Completed -> Set(Recovering),
        Recovering -> Set(UnInitialized, Ready, Running, Pausing, Paused, Completed)
      ),
      UnInitialized
    ) {

  private var isStarted = false

}