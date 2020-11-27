package edu.uci.ics.amber.engine.architecture.worker.neo

import java.util.concurrent.CompletableFuture

object PauseUtil{
  // TODO: check if this is necessary
  // I want to introduce pause privileges so that stronger pause can override weak pause
  // suppose:
  // 1. an internal control pauses the workflow (with privilege 1)
  // 2. user pauses the workflow (with privilege 2)
  // 3. the internal control resumes the workflow (with privilege 1)
  // step 3 will not be done since the user pauses the workflow.
  final val NoPause = 0
  final val BackPressure = 1
  final val Breakpoint = 2
  final val CoreException = 3
  final val Internal = 4
  final val User = 1024
  final val Recovery = 2048
  final val Forced = 9999
}

trait PauseUtil {

  // current pause privilege level
  var pausePrivilegeLevel: Int = PauseUtil.NoPause
  // yielded control of the dp thread
  var currentFuture: CompletableFuture[Void] = _


  /** pause functionality (synchronized)
    * both dp thread and actor can call this function
    * @param level
    */
  def pause(level:Int): Unit = {
    synchronized{
      if(level >= pausePrivilegeLevel){
        this.pausePrivilegeLevel = level
      }
    }
  }

  /** resume functionality (not synchronized)
    * only actor calls this function for now
    * @param level
    */
  def resume(level:Int): Unit ={
    if(level < pausePrivilegeLevel) {
      return
    }
    // only privilege level >= current pause privilege level can resume the worker
    pausePrivilegeLevel = PauseUtil.NoPause
    // If dp thread suspended, release it
    if (this.currentFuture != null) {
      this.currentFuture.complete(null)
      this.currentFuture = null
    }
  }

  /** check for pause in dp thread
    * only dp thread and operator logic can call this function
    * @throws
    */
  @throws[Exception]
  def pauseCheck(): Unit = {
    // returns if not paused
    if (this.pausePrivilegeLevel == PauseUtil.NoPause) return
    // create a future and wait for its completion
    this.currentFuture = new CompletableFuture[Void]
    // thread blocks here
    this.currentFuture.get
  }

}
