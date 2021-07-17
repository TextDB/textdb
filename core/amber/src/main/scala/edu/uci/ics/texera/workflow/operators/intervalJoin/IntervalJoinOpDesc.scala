package edu.uci.ics.texera.workflow.operators.intervalJoin

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonPropertyDescription}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.google.common.base.Preconditions
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle
import edu.uci.ics.amber.engine.common.virtualidentity.{LinkIdentity, OperatorIdentity}
import edu.uci.ics.amber.engine.operators.OpExecConfig
import edu.uci.ics.texera.workflow.common.metadata.annotations.{
  AutofillAttributeName,
  AutofillAttributeNameOnPort1
}
import edu.uci.ics.texera.workflow.common.metadata.{
  InputPort,
  OperatorGroupConstants,
  OperatorInfo,
  OutputPort
}
import edu.uci.ics.texera.workflow.common.operators.{ManyToOneOpExecConfig, OperatorDescriptor}
import edu.uci.ics.texera.workflow.common.tuple.schema.{Attribute, OperatorSchemaInfo, Schema}

/** This Operator have two assumptions:
  * 1. The tuples in both inputs come in ascending order
  * 2. The left input join key takes as points, join condition is: left key in the range of (right key, right key + constant)
  */
class IntervalJoinOpDesc extends OperatorDescriptor {

  @JsonProperty(required = true)
  @JsonSchemaTitle("Left Input attr")
  @JsonPropertyDescription("Choose one attribute in the left table")
  @AutofillAttributeName
  var leftAttributeName: String = _

  @JsonProperty(required = true)
  @JsonSchemaTitle("Right Input attr")
  @JsonPropertyDescription("Choose one attribute in the right table")
  @AutofillAttributeNameOnPort1
  var rightAttributeName: String = _

  @JsonProperty(required = true, defaultValue = "10")
  @JsonSchemaTitle("Interval Constant")
  @JsonPropertyDescription("left attri in (right, right + constant)")
  var constant: Long = 10

  @JsonProperty(required = true, defaultValue = "true")
  @JsonSchemaTitle("Include Left Bound")
  @JsonPropertyDescription("Include condition left attri = right attri")
  var includeLeftBound: Boolean = true

  @JsonProperty(required = true, defaultValue = "true")
  @JsonSchemaTitle("Include Right Bound")
  @JsonPropertyDescription("Include condition left attri = right attri")
  var includeRightBound: Boolean = true

  @JsonDeserialize(contentAs = classOf[TimeIntervalType])
  @JsonProperty(defaultValue = "day", required = false)
  @JsonSchemaTitle("Time interval type")
  @JsonPropertyDescription("Year, Month, Day, Hour, Minute or Second")
  var timeIntervalType: Option[TimeIntervalType] = _

  @JsonIgnore
  var opExecConfig: ManyToOneOpExecConfig = _

  @JsonIgnore
  var leftInpueLink: LinkIdentity = _

  override def operatorExecutor(operatorSchemaInfo: OperatorSchemaInfo): OpExecConfig = {

    var config = new ManyToOneOpExecConfig(
      operatorIdentifier,
      _ =>
        new IntervalJoinOpExec(
          operatorSchemaInfo,
          this
        )
    )
    leftInpueLink = config.inputToOrdinalMapping.find(pair => pair._2 == 0).get._1
    config

  }

  override def operatorInfo: OperatorInfo =
    OperatorInfo(
      "Interval Join",
      "Join two inputs with left table join key in the range of [right table join key, right table join key + constant value]",
      OperatorGroupConstants.JOIN_GROUP,
      inputPorts = List(InputPort("left table"), InputPort("right table")),
      outputPorts = List(OutputPort())
    )

  def this(
      isLeftTableInput: LinkIdentity,
      leftTableAttributeName: String,
      rightTableAttributeName: String,
      schemas: Array[Schema],
      constant: Long,
      includeLeftBound: Boolean,
      includeRightBound: Boolean,
      timeIntervalType: TimeIntervalType
  ) {
    this() // Calling primary constructor, and it is first line
    this.leftAttributeName = leftTableAttributeName
    this.rightAttributeName = rightTableAttributeName
    this.constant = constant
    this.includeLeftBound = includeLeftBound
    this.includeRightBound = includeRightBound
    this.timeIntervalType = Some(timeIntervalType)
    this.opExecConfig = new ManyToOneOpExecConfig(
      OperatorIdentity("test", "test"),
      _ =>
        new IntervalJoinOpExec(
          OperatorSchemaInfo(schemas, getOutputSchema(schemas)),
          this
        )
    )
    this.leftInpueLink = isLeftTableInput
  }

  override def getOutputSchema(schemas: Array[Schema]): Schema = {
    Preconditions.checkArgument(schemas.length == 2)
    val builder: Schema.Builder = Schema.newBuilder()
    var leftTableSchema: Schema = schemas(0)
    var rightTableSchema: Schema = schemas(1)
    builder.add(leftTableSchema)
    rightTableSchema.getAttributesScala
      .map(attr => {
        if (leftTableSchema.containsAttribute(attr.getName)) {
          builder.add(new Attribute(s"${attr.getName}#@1", attr.getType))
        } else {
          builder.add(attr.getName, attr.getType)
        }
      })
    builder.build()
  }

}
