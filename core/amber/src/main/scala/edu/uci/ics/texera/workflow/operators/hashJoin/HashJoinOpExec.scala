package edu.uci.ics.texera.workflow.operators.hashJoin

import edu.uci.ics.amber.engine.common.amberexception.WorkflowRuntimeException
import edu.uci.ics.amber.engine.common.{InputExhausted, WorkflowLogger}
import edu.uci.ics.amber.engine.common.ambertag.{LayerTag, OperatorIdentifier}
import edu.uci.ics.amber.error.WorkflowRuntimeError
import edu.uci.ics.texera.workflow.common.operators.OperatorExecutor
import edu.uci.ics.texera.workflow.common.tuple.Tuple
import edu.uci.ics.texera.workflow.common.tuple.schema.{Attribute, Schema}
import org.apache.avro.SchemaBuilder

import java.util
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class HashJoinOpExec[K](val opDesc: HashJoinOpDesc[K]) extends OperatorExecutor {

  var hashJoinOpExecLogger = new WorkflowLogger(
    s"${opDesc.operatorIdentifier.getGlobalIdentity}-HashJoinOpExec"
  )

  var isBuildTableFinished: Boolean = false
  var buildTableHashMap: mutable.HashMap[K, ArrayBuffer[Tuple]] = _
  var outputProbeSchema: Schema = _

  var currentEntry: Iterator[Tuple] = _
  var currentTuple: Tuple = _

  // probe attribute removed in the output schema
  private def createOutputProbeSchema(buildTuple: Tuple, probeTuple: Tuple): Schema = {
    val buildSchema = buildTuple.getSchema()
    val probeSchema = probeTuple.getSchema()
    var builder = Schema.newBuilder()
    probeSchema
      .getAttributes()
      .forEach(attr => {
        if (attr.getName() != opDesc.probeAttributeName) {
          if (buildSchema.containsAttribute(attr.getName())) {
            builder.add(new Attribute(s"${attr.getName()}#@1", attr.getType()))
          } else {
            builder.add(attr)
          }
        }
      })
    builder.build()
  }

  override def processTexeraTuple(
      tuple: Either[Tuple, InputExhausted],
      input: OperatorIdentifier
  ): Iterator[Tuple] = {
    tuple match {
      case Left(t) =>
        // The operatorInfo() in HashJoinOpDesc has a inputPorts list. In that the
        // small input port comes first. So, it is assigned the inputNum 0. Similarly
        // the large input is assigned the inputNum 1.
        if (opDesc.opExecConfig.getInputNum(input) == 0) {
          val key = t.getField(opDesc.buildAttributeName).asInstanceOf[K]
          var storedTuples = buildTableHashMap.getOrElse(key, new ArrayBuffer[Tuple]())
          storedTuples += t
          buildTableHashMap.put(key, storedTuples)
          Iterator()
        } else {
          if (!isBuildTableFinished) {
            val err = WorkflowRuntimeError(
              "Probe table came before build table ended",
              "HashJoinOpExec",
              Map("stacktrace" -> Thread.currentThread().getStackTrace().mkString("\n"))
            )
            hashJoinOpExecLogger.logError(err)
            throw new WorkflowRuntimeException(err)
          } else {
            val key = t.getField(opDesc.probeAttributeName).asInstanceOf[K]
            val storedTuples = buildTableHashMap.getOrElse(key, new ArrayBuffer[Tuple]())
            var tuplesToOutput: ArrayBuffer[Tuple] = new ArrayBuffer[Tuple]()
            if (storedTuples.size == 0) {
              Iterator()
            }
            if (outputProbeSchema == null) {
              outputProbeSchema = createOutputProbeSchema(storedTuples(0), t)
            }

            storedTuples.foreach(buildTuple => {
              val builder = Tuple
                .newBuilder()
                .add(buildTuple)

              var newProbeIdx = 0
              // outputProbeSchema doesnt have "probeAttribute" but t does. The following code
              //  takes that into consideration while creating a tuple.
              for (i <- 0 to t.getFields().size() - 1) {
                if (!t.getSchema().getAttributeNames().get(i).equals(opDesc.probeAttributeName)) {
                  builder.add(
                    outputProbeSchema.getAttributes().get(newProbeIdx),
                    t.getFields().get(i)
                  )
                  newProbeIdx += 1
                }
              }

              tuplesToOutput += builder.build()
            })
            tuplesToOutput.iterator
          }
        }
      case Right(_) =>
        if (opDesc.opExecConfig.getInputNum(input) == 0) {
          isBuildTableFinished = true
        }
        Iterator()

    }
  }

  override def open(): Unit = {
    buildTableHashMap = new mutable.HashMap[K, mutable.ArrayBuffer[Tuple]]()
  }

  override def close(): Unit = {
//    buildTableInputNum = -1
    buildTableHashMap.clear()
  }
}