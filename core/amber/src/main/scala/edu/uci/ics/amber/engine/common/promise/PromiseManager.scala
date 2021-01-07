package edu.uci.ics.amber.engine.common.promise

import com.twitter.util.{Future, Promise}
import com.typesafe.scalalogging.LazyLogging
import edu.uci.ics.amber.engine.architecture.messaginglayer.ControlOutputPort
import edu.uci.ics.amber.engine.common.ambertag.neo.VirtualIdentity.ActorVirtualIdentity

import scala.collection.mutable

/** This is the central unit of handling promises in actors.
  * @param selfID
  * @param controlOutputPort
  */
class PromiseManager(selfID: ActorVirtualIdentity, controlOutputPort: ControlOutputPort)
    extends LazyLogging {

  // the monotonically increasing identifier
  // for promises created by this promise manager
  protected var promiseID = 0L

  // promises that created by this promise manager will be put
  // into this map, send to the destination, and wait for the
  // return value.
  protected val unCompletedPromises = new mutable.HashMap[PromiseContext, WorkflowPromise[_]]()

  // this set holds promises for groups.
  protected val unCompletedGroupPromises = new mutable.HashSet[GroupedWorkflowPromise[_]]()

  // context for current executing promise.
  protected var promiseContext: PromiseContext = _

  // synchronous promises will be enqueued.
  protected val queuedInvocations = new mutable.Queue[PromisePayload]()

  // save all synchronous promises with the same root.
  protected val ongoingSyncPromises = new mutable.HashSet[PromiseContext]()

  // the root context for the current synchronous promise.
  protected var syncPromiseRoot: PromiseContext = _

  // empty promise handler.
  // default behavior: discard
  protected var promiseHandler: PartialFunction[PromiseBody[_], Unit] = {
    case promise =>
      logger.info(s"discarding $promise")
  }

  // process one promise message.
  def execute(payload: PromisePayload): Unit = {
    payload match {
      // handle return value
      case ret: ReturnPayload =>
        // if the return value corresponds to one of the
        // promises created by this manager.
        if (unCompletedPromises.contains(ret.context)) {
          val p = unCompletedPromises(ret.context)
          // get the context
          promiseContext = p.ctx
          ret.returnValue match {
            case throwable: Throwable =>
              // if an error is returned
              p.setException(throwable)
            case _ =>
              // otherwise, we put the return value
              // and invoke the callback.
              p.setValue(ret.returnValue.asInstanceOf[p.returnType])
          }
          // the promise is resolved
          unCompletedPromises.remove(ret.context)
        }

        // check if the return value belongs to group promises
        for (i <- unCompletedGroupPromises) {
          if (i.takeReturnValue(ret)) {
            // all return values are collected, set the context
            promiseContext = i.promise.ctx
            // invoke callback
            i.invoke()
            // the group promise is resolved
            unCompletedGroupPromises.remove(i)
          }
        }

      // handle root synchronous promise
      case PromiseInvocation(
            ctx: RootPromiseContext,
            call: PromiseBody[_] with SynchronizedInvocation
          ) =>
        if (syncPromiseRoot == null) {
          // if there is no other executing synchronous promise,
          // execute this one
          registerSyncPromise(ctx, ctx)
          invokePromise(ctx, call)
        } else {
          // otherwise, enqueue it
          queuedInvocations.enqueue(payload)
        }

      // handle synchronous promise created by other promise
      case PromiseInvocation(
            ctx: ChildPromiseContext,
            call: PromiseBody[_] with SynchronizedInvocation
          ) =>
        if (syncPromiseRoot == null || ctx.root == syncPromiseRoot) {
          // if there is no other executing synchronous promise,
          // or this promise has same root context (chain of promises),
          // execute this one.
          registerSyncPromise(ctx.root, ctx)
          invokePromise(ctx, call)
        } else {
          // otherwise, enqueue it
          queuedInvocations.enqueue(payload)
        }

      // trivial case
      case p: PromiseInvocation =>
        // set context
        promiseContext = p.context
        try {
          // execute it
          promiseHandler(p.call)
        } catch {
          case e: Throwable =>
            // if error occurs, return it to the sender.
            returning(e)
        }
    }

    // execute queued sync promises
    tryInvokeNextSyncPromise()
  }

  // send a control message to another actor, and keep the handle.
  def schedule[T](cmd: PromiseBody[T], on: ActorVirtualIdentity = selfID): Promise[T] = {
    val ctx = mkPromiseContext()
    promiseID += 1
    controlOutputPort.sendTo(on, PromiseInvocation(ctx, cmd))
    val promise = WorkflowPromise[T](promiseContext)
    unCompletedPromises(ctx) = promise
    promise
  }

  // send a grouped control message to other actors, and keep the handle.
  def schedule[T](seq: (PromiseBody[T], ActorVirtualIdentity)*): Promise[Seq[T]] = {
    val promise = WorkflowPromise[Seq[T]](promiseContext)
    if (seq.isEmpty) {
      // if the sequence is empty, resolve the promise immediately
      promise.setValue(Seq.empty)
    } else {
      unCompletedGroupPromises.add(
        GroupedWorkflowPromise[T](promiseID, promiseID + seq.length, promise)
      )
      seq.foreach {
        case (body, virtualIdentity) =>
          val ctx = mkPromiseContext()
          promiseID += 1
          controlOutputPort.sendTo(virtualIdentity, PromiseInvocation(ctx, body))
      }
    }
    promise
  }

  def returning(value: Any): Unit = {
    // returning should be used at most once per context
    if (promiseContext != null) {
      controlOutputPort.sendTo(promiseContext.sender, ReturnPayload(promiseContext, value))
      exitCurrentPromise()
    }
  }

  def returning(): Unit = {
    // returning should be used at most once per context
    if (promiseContext != null) {
      controlOutputPort.sendTo(
        promiseContext.sender,
        ReturnPayload(promiseContext, PromiseCompleted())
      )
      exitCurrentPromise()
    }
  }

  @inline
  private def exitCurrentPromise(): Unit = {
    if (ongoingSyncPromises.contains(promiseContext)) {
      ongoingSyncPromises.remove(promiseContext)
    }
    promiseContext = null
  }

  @inline
  private def tryInvokeNextSyncPromise(): Unit = {
    if (ongoingSyncPromises.isEmpty) {
      syncPromiseRoot = null
      if (queuedInvocations.nonEmpty) {
        execute(queuedInvocations.dequeue())
      }
    }
  }

  @inline
  private def registerSyncPromise(rootCtx: RootPromiseContext, ctx: PromiseContext): Unit = {
    syncPromiseRoot = rootCtx
    ongoingSyncPromises.add(ctx)
  }

  @inline
  protected def mkPromiseContext(): PromiseContext = {
    promiseContext match {
      case null =>
        // if current context is null, create root context
        PromiseContext(selfID, promiseID)
      case ctx: RootPromiseContext =>
        // create child context with root
        PromiseContext(selfID, promiseID, ctx)
      case ctx: ChildPromiseContext =>
        // use ctx.root to create child context
        PromiseContext(selfID, promiseID, ctx.root)
    }
  }

  @inline
  private def invokePromise(ctx: PromiseContext, call: PromiseBody[_]): Unit = {
    promiseContext = ctx
    promiseHandler(call)
  }

}