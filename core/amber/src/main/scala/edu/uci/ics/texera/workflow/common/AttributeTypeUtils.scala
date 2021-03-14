package edu.uci.ics.texera.workflow.common

import edu.uci.ics.texera.workflow.common.tuple.Tuple
import edu.uci.ics.texera.workflow.common.tuple.schema.{Attribute, AttributeType, Schema}
import edu.uci.ics.texera.workflow.common.tuple.schema.AttributeType._

import java.sql.Timestamp
import java.time.Instant
import java.time.format.DateTimeParseException
import scala.util.control.Exception.allCatch

object AttributeTypeUtils extends Serializable {

  /**
    * this loop check whether the current attribute in the array is the attribute for casting,
    * if it is, change it to result type
    * if it's not, remain the same type
    * we need this loop to keep the order the same as the original
    * @param schema schema of data
    * @param attribute selected attribute
    * @param resultType casting type
    * @return schema of data
    */
  def SchemaCasting(
      schema: Schema,
      attribute: String,
      resultType: AttributeType
  ): Schema = {
    // need a builder to maintain the order of original schema
    val builder = Schema.newBuilder
    val attributes: List[Attribute] = schema.getAttributesScala
    // change the schema when meet selected attribute else remain the same
    for (i <- attributes.indices) {
      if (attributes.apply(i).getName.equals(attribute)) {
        resultType match {
          case STRING | INTEGER | DOUBLE | LONG | BOOLEAN =>
            builder.add(attribute, resultType)
          case TIMESTAMP | ANY | _ =>
            builder.add(attribute, STRING)
        }
      } else {
        builder.add(attributes.apply(i).getName, attributes.apply(i).getType)
      }
    }
    builder.build()
  }

  /**
    * Casting the tuple and return a new tuple with casted type
    * @param tuple tuple to be processed
    * @param attribute selected attribute
    * @param resultType casting type
    * @return casted tuple
    */
  def TupleCasting(
      tuple: Tuple,
      attribute: String,
      resultType: AttributeType
  ): Tuple = {
    // need a builder to maintain the order of original tuple
    val builder: Tuple.Builder = Tuple.newBuilder
    val attributes: List[Attribute] = tuple.getSchema.getAttributesScala
    // change the tuple when meet selected attribute else remain the same
    for (i <- attributes.indices) {
      if (attributes.apply(i).getName.equals(attribute)) {
        val field: String = tuple.get(i).toString
        resultType match {
          case STRING    => builder.add(attribute, resultType, field)
          case INTEGER   => builder.add(attribute, resultType, field.toInt)
          case DOUBLE    => builder.add(attribute, resultType, field.toDouble)
          case LONG      => builder.add(attribute, resultType, field.toLong)
          case BOOLEAN   => builder.add(attribute, resultType, field.toBoolean)
          case TIMESTAMP => builder.add(attribute, STRING, field)
          case ANY       => builder.add(attribute, STRING, field)
          case _         => builder.add(attribute, resultType, field)
        }
      } else {
        builder.add(attributes.apply(i).getName, attributes.apply(i).getType, tuple.get(i))
      }
    }
    builder.build()
  }

  /**
    * parse Field to a corresponding Java object base on the given Schema AttributeType
    * @param attributeTypes Schema AttributeTypeList
    * @param fields fields value, originally is String
    * @return parsedFields
    */
  def parseField(
      attributeTypes: Array[AttributeType],
      fields: Array[String]
  ): Array[Object] = {
    val parsedFields: Array[Object] = new Array[Object](fields.length)
    for (i <- fields.indices) {
      attributeTypes.apply(i) match {
        case INTEGER => parsedFields.update(i, Integer.valueOf(fields.apply(i)))
        case LONG    => parsedFields.update(i, java.lang.Long.valueOf(fields.apply(i)))
        case DOUBLE =>
          parsedFields.update(i, java.lang.Double.valueOf(fields.apply(i)))
        case BOOLEAN =>
          parsedFields.update(i, java.lang.Boolean.valueOf(fields.apply(i)))
        case STRING    => parsedFields.update(i, fields.apply(i))
        case TIMESTAMP =>
        case ANY       =>
        case _         => parsedFields.update(i, fields.apply(i))
      }
    }
    parsedFields
  }

  /**
    * Infers field types of a given row of data. The given attributeTypes will be updated
    * through each iteration of row inference, to contain the must accurate inference.
    * @param attributeTypes AttributeTypes that being passed to each iteration.
    * @param fields data fields to be parsed, originally as String fields
    * @return
    */
  def inferRow(
      attributeTypes: Array[AttributeType],
      fields: Array[String]
  ): Unit = {
    for (i <- fields.indices) {
      attributeTypes.update(i, inferField(attributeTypes.apply(i), fields.apply(i)))
    }
  }

  /**
    * Infers field types of a given row of data.
    * @param fields data fields to be parsed, originally as String fields
    * @return AttributeType array
    */
  def inferRow(fields: Array[String]): Array[AttributeType] = {
    val attributeTypes: Array[AttributeType] =
      Array.fill[AttributeType](fields.length)(INTEGER)
    for (i <- fields.indices) {
      attributeTypes.update(i, inferField(fields.apply(i)))
    }
    attributeTypes
  }

  /**
    * infer filed type with only data field
    * @param fieldValue data field to be parsed, original as String field
    * @return inferred AttributeType
    */
  def inferField(fieldValue: String): AttributeType = {
    tryParseInteger(fieldValue)
  }

  /**
    * InferField when get both typeSofar and tuple string
    * @param attributeType typeSofar
    * @param fieldValue data field to be parsed, original as String field
    * @return inferred AttributeType
    */
  def inferField(attributeType: AttributeType, fieldValue: String): AttributeType = {
    attributeType match {
      case STRING  => tryParseString()
      case BOOLEAN => tryParseBoolean(fieldValue)
      case DOUBLE  => tryParseDouble(fieldValue)
      case LONG    => tryParseLong(fieldValue)
      case INTEGER => tryParseInteger(fieldValue)
      case _       => tryParseString()
    }
  }

  @throws[AttributeTypeException]
  def parseInteger(fieldValue: Object): Integer = {
    fieldValue match {
      case str: String                => str.toInt
      case int: Integer               => int
      case long: java.lang.Long       => long.toInt
      case double: java.lang.Double   => double.toInt
      case boolean: java.lang.Boolean => if (boolean) 1 else 0
      // Timestamp is considered to be illegal here.
      case _ =>
        throw new AttributeTypeException(
          s"not able to parse type ${fieldValue.getClass} to Integer."
        )
    }
  }

  @throws[AttributeTypeException]
  def parseBoolean(fieldValue: Object): java.lang.Boolean = {
    fieldValue match {
      case str: String                => str.toBoolean
      case int: Integer               => int != 0
      case long: java.lang.Long       => long != 0
      case double: java.lang.Double   => double != 0
      case boolean: java.lang.Boolean => boolean
      // Timestamp is considered to be illegal here.
      case _ =>
        throw new AttributeTypeException(
          s"not able to parse type ${fieldValue.getClass} to Boolean."
        )
    }
  }

  @throws[AttributeTypeException]
  def parseLong(fieldValue: Object): java.lang.Long = {
    fieldValue match {
      case str: String                => str.toLong
      case int: Integer               => int.toLong
      case long: java.lang.Long       => long
      case double: java.lang.Double   => double.toLong
      case boolean: java.lang.Boolean => if (boolean) 1L else 0L
      case timestamp: Timestamp       => timestamp.toInstant.toEpochMilli
      case _ =>
        throw new AttributeTypeException(s"not able to parse type ${fieldValue.getClass} to Long.")
    }
  }

  @throws[AttributeTypeException]
  def parseDouble(fieldValue: Object): java.lang.Double = {
    fieldValue match {
      case str: String                => str.toDouble
      case int: Integer               => int.toDouble
      case long: java.lang.Long       => long.toDouble
      case double: java.lang.Double   => double
      case boolean: java.lang.Boolean => if (boolean) 1 else 0
      // Timestamp is considered to be illegal here.
      case _ =>
        throw new AttributeTypeException(
          s"not able to parse type ${fieldValue.getClass} to Double."
        )
    }
  }

  @throws[AttributeTypeException]
  def parseTimestamp(fieldValue: Object): Timestamp = {
    fieldValue match {
      case str: String =>
        try new Timestamp(Instant.parse(str).toEpochMilli)
        catch {
          case _: DateTimeParseException => Timestamp.valueOf(str)
        }
      case long: java.lang.Long => new Timestamp(long)
      // Integer, Long, Double and Boolean are considered to be illegal here.
      case _ =>
        throw new AttributeTypeException(
          s"not able to parse type ${fieldValue.getClass} to Timestamp."
        )
    }
  }

  private def tryParseInteger(fieldValue: String): AttributeType = {
    allCatch opt parseInteger(fieldValue) match {
      case Some(_) => INTEGER
      case None    => tryParseLong(fieldValue)
    }
  }

  private def tryParseLong(fieldValue: String): AttributeType = {
    allCatch opt parseLong(fieldValue) match {
      case Some(_) => LONG
      case None    => tryParseDouble(fieldValue)
    }
  }

  private def tryParseDouble(fieldValue: String): AttributeType = {
    allCatch opt parseDouble(fieldValue) match {
      case Some(_) => DOUBLE
      case None    => tryParseBoolean(fieldValue)
    }
  }

  private def tryParseBoolean(fieldValue: String): AttributeType = {
    allCatch opt parseBoolean(fieldValue) match {
      case Some(_) => BOOLEAN
      case None    => tryParseString()
    }
  }

  private def tryParseString(): AttributeType = {
    STRING
  }

  class AttributeTypeException(msg: String) extends IllegalArgumentException(msg) {}
}
