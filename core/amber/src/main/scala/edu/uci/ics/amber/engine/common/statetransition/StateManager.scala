package edu.uci.ics.amber.engine.common.statetransition

import edu.uci.ics.amber.engine.common.statetransition.StateManager.{
  IntermediateState,
  InvalidStateException,
  InvalidTransitionException
}

import scala.collection.mutable

object StateManager {
  case class InvalidStateException(message: String)
      extends RuntimeException(message)
      with Serializable
  case class InvalidTransitionException(message: String)
      extends RuntimeException(message)
      with Serializable

  trait IntermediateState
}

class StateManager[T](stateTransitionGraph: Map[T, Set[T]], initialState: T) {

  private var currentState: T = initialState
  private val stateStack = mutable.Stack[T]()

  if (!initialState.isInstanceOf[IntermediateState]) {
    stateStack.push(initialState)
  }

  def shouldBe(state: T): Unit = {
    if (currentState != state) {
      throw InvalidStateException(s"except state = $state but current state = $currentState")
    }
  }

  def shouldBe(states: T*): Unit = {
    if (!states.contains(currentState)) {
      throw InvalidStateException(
        s"except state in [${states.mkString(",")}] but current state = $currentState"
      )
    }
  }

  def transitTo(state: T, discardOldStates: Boolean = true): Unit = {
    if (state == currentState) {
      return
      // throw InvalidTransitionException(s"current state is already $currentState")
    }
    if (discardOldStates) {
      stateStack.clear()
    }
    if (!state.isInstanceOf[IntermediateState]) {
      stateStack.push(state)
    }
    if (!stateTransitionGraph.getOrElse(state, Set()).contains(state)) {
      throw InvalidTransitionException(s"cannot transit from $currentState to $state")
    }
    currentState = state
  }

  def backToPreviousState(): Unit = {
    if (stateStack.isEmpty) {
      throw InvalidTransitionException(s"there is no previous state for $currentState")
    }
    currentState = stateStack.pop()
  }

  def getCurrentState: T = currentState

}