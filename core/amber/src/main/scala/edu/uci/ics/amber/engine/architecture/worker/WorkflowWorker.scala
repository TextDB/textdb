package edu.uci.ics.amber.engine.architecture.worker

import akka.actor.{ActorRef, Props}
import akka.util.Timeout
import com.softwaremill.macwire.wire
import edu.uci.ics.amber.engine.architecture.common.WorkflowActor
import edu.uci.ics.amber.engine.architecture.controller.promisehandlers.WorkerExecutionStartedHandler.WorkerStateUpdated
import edu.uci.ics.amber.engine.architecture.messaginglayer.NetworkCommunicationActor.{
  NetworkMessage,
  RegisterActorRef
}
import edu.uci.ics.amber.engine.architecture.messaginglayer.{
  BatchToTupleConverter,
  DataOutputPort,
  NetworkInputPort,
  TupleToBatchConverter
}
import edu.uci.ics.amber.engine.common.IOperatorExecutor
import edu.uci.ics.amber.engine.common.ambermessage.{
  ControlPayload,
  DataPayload,
  WorkflowControlMessage,
  WorkflowDataMessage
}
import edu.uci.ics.amber.engine.common.rpc.AsyncRPCClient.{ControlInvocation, ReturnPayload}
import edu.uci.ics.amber.engine.common.rpc.{AsyncRPCClient, AsyncRPCHandlerInitializer}
import edu.uci.ics.amber.engine.common.statetransition.WorkerStateManager
import edu.uci.ics.amber.engine.common.statetransition.WorkerStateManager._
import edu.uci.ics.amber.engine.common.virtualidentity.{ActorVirtualIdentity, VirtualIdentity}
import edu.uci.ics.amber.engine.recovery.DataLogManager.DataLogElement
import edu.uci.ics.amber.engine.recovery.{
  ControlLogManager,
  DPLogManager,
  DataLogManager,
  EmptyLogStorage,
  LogStorage
}
import edu.uci.ics.amber.error.ErrorUtils.safely
import edu.uci.ics.amber.error.WorkflowRuntimeError

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object WorkflowWorker {
  def props(
      id: ActorVirtualIdentity,
      op: IOperatorExecutor,
      parentNetworkCommunicationActorRef: ActorRef,
      controlLogStorage: LogStorage[WorkflowControlMessage] = new EmptyLogStorage(),
      dataLogStorage: LogStorage[DataLogElement] = new EmptyLogStorage(),
      dpLogStorage: LogStorage[Long] = new EmptyLogStorage()
  ): Props =
    Props(
      new WorkflowWorker(
        id,
        op,
        parentNetworkCommunicationActorRef,
        controlLogStorage,
        dataLogStorage,
        dpLogStorage
      )
    )
}

class WorkflowWorker(
    identifier: ActorVirtualIdentity,
    operator: IOperatorExecutor,
    parentNetworkCommunicationActorRef: ActorRef,
    controlLogStorage: LogStorage[WorkflowControlMessage] = new EmptyLogStorage(),
    dataLogStorage: LogStorage[DataLogElement] = new EmptyLogStorage(),
    dpLogStorage: LogStorage[Long] = new EmptyLogStorage()
) extends WorkflowActor(identifier, parentNetworkCommunicationActorRef) {
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout: Timeout = 5.seconds

  val workerStateManager: WorkerStateManager = new WorkerStateManager()

  lazy val dataLogManager: DataLogManager = wire[DataLogManager]
  lazy val dpLogManager: DPLogManager = wire[DPLogManager]

  lazy val pauseManager: PauseManager = wire[PauseManager]
  lazy val dataProcessor: DataProcessor = wire[DataProcessor]
  lazy val dataInputPort: NetworkInputPort[DataPayload] =
    new NetworkInputPort[DataPayload](this.logger, this.handleDataPayload)
  lazy val controlInputPort: NetworkInputPort[ControlPayload] =
    new NetworkInputPort[ControlPayload](this.logger, this.handleControlPayload)
  lazy val dataOutputPort: DataOutputPort = wire[DataOutputPort]
  lazy val batchProducer: TupleToBatchConverter = wire[TupleToBatchConverter]
  lazy val tupleProducer: BatchToTupleConverter = wire[BatchToTupleConverter]
  lazy val breakpointManager: BreakpointManager = wire[BreakpointManager]

  val rpcHandlerInitializer: AsyncRPCHandlerInitializer =
    wire[WorkerAsyncRPCHandlerInitializer]

  if (parentNetworkCommunicationActorRef != null) {
    parentNetworkCommunicationActorRef ! RegisterActorRef(identifier, self)
  }

  workerStateManager.assertState(Uninitialized)
  workerStateManager.transitTo(Ready)

  val controlLogManager: ControlLogManager = wire[ControlLogManager]

  dataLogManager.onComplete(() => {
    context.become(receiveAndProcessMessages)
    unstashAll()
  })

  def recovering: Receive = {
    disallowActorRefRelatedMessages orElse
      receiveDataMessages orElse
      stashControlMessages orElse
      logUnhandledMessages
  }

  override def receive: Receive = recovering

  def receiveAndProcessMessages: Receive = {
    disallowActorRefRelatedMessages orElse
      receiveDataMessages orElse
      receiveControlMessages orElse {
      case other =>
        logger.logError(
          WorkflowRuntimeError(s"unhandled message: $other", identifier.toString, Map.empty)
        )
    }
  }

  final def receiveDataMessages: Receive = {
    case NetworkMessage(id, WorkflowDataMessage(from, seqNum, payload)) =>
      dataInputPort.handleMessage(this.sender(), id, from, seqNum, payload)
  }

  def receiveControlMessages: Receive = {
    case NetworkMessage(id, cmd @ WorkflowControlMessage(from, seqNum, payload)) =>
      controlLogManager.persistControlMessage(cmd)
      try {
        // use control input port to pass control messages
        controlInputPort.handleMessage(this.sender(), id, from, seqNum, payload)
      } catch safely {
        case e =>
          logger.logError(WorkflowRuntimeError(e, identifier.toString))
      }
  }

  def stashControlMessages: Receive = {
    case msg @ NetworkMessage(id, cmd: WorkflowControlMessage) =>
      stash()
  }

  override def postStop(): Unit = {
    // shutdown dp thread by sending a command
    dataProcessor.enqueueCommand(ShutdownDPThread(), ActorVirtualIdentity.Self)
    super.postStop()
  }

  final def handleDataPayload(from: VirtualIdentity, dataPayload: DataPayload): Unit = {
    dataLogManager.filterMessage(from, dataPayload).foreach {
      case (vid, payload) =>
        tupleProducer.processDataPayload(vid, payload)
    }
  }

  final def handleControlPayload(from: VirtualIdentity, controlPayload: ControlPayload): Unit = {
    // let dp thread process it
    assert(from.isInstanceOf[ActorVirtualIdentity])
    controlPayload match {
      case controlCommand @ (ControlInvocation(_, _) | ReturnPayload(_, _)) =>
        dataProcessor.enqueueCommand(controlCommand, from)
      case _ =>
        logger.logError(
          WorkflowRuntimeError(
            s"unhandled control payload: $controlPayload",
            identifier.toString,
            Map.empty
          )
        )
    }
  }

}
