package edu.uci.ics.amber.engine.common

import edu.uci.ics.amber.engine.common.ambertag.OperatorIdentifier
import edu.uci.ics.amber.engine.common.tuple.ITuple

case class InputExhausted()

trait IOperatorExecutor {

  def open(): Unit

  def close(): Unit

  def processTuple(
      tuple: Either[ITuple, InputExhausted],
      input: OperatorIdentifier
  ): Iterator[ITuple]

  def getParam(query: String): String = { null }

}
