// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO2

package edu.uci.ics.amber.engine.common.virtualidentity

/** final case class WorkflowIdentity (
  *    id: String
  * )
  */
@SerialVersionUID(0L)
final case class WorkflowIdentity(
    id: _root_.scala.Predef.String,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[WorkflowIdentity] {
    @transient
    private[this] var __serializedSizeCachedValue: _root_.scala.Int = 0
    private[this] def __computeSerializedValue(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = id
        __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
      };
      __size += unknownFields.serializedSize
      __size
    }
    override def serializedSize: _root_.scala.Int = {
      var read = __serializedSizeCachedValue
      if (read == 0) {
        read = __computeSerializedValue()
        __serializedSizeCachedValue = read
      }
      read
    }
    def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): _root_.scala.Unit = {
      
      {
        val __v = id
        _output__.writeString(1, __v)
      };
      unknownFields.writeTo(_output__)
    }
    def withId(__v: _root_.scala.Predef.String): WorkflowIdentity = copy(id = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => id
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(id)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion = edu.uci.ics.amber.engine.common.virtualidentity.WorkflowIdentity
    // @@protoc_insertion_point(GeneratedMessage[edu.uci.ics.amber.engine.common.WorkflowIdentity])
}

object WorkflowIdentity extends scalapb.GeneratedMessageCompanion[edu.uci.ics.amber.engine.common.virtualidentity.WorkflowIdentity] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[edu.uci.ics.amber.engine.common.virtualidentity.WorkflowIdentity] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): edu.uci.ics.amber.engine.common.virtualidentity.WorkflowIdentity = {
    var __requiredFields0: _root_.scala.Long = 0x1L
    var __id: _root_.scala.Predef.String = ""
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __id = _input__.readStringRequireUtf8()
          __requiredFields0 &= 0xfffffffffffffffeL
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    if (__requiredFields0 != 0L) { throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.") } 
    edu.uci.ics.amber.engine.common.virtualidentity.WorkflowIdentity(
        id = __id,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[edu.uci.ics.amber.engine.common.virtualidentity.WorkflowIdentity] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      edu.uci.ics.amber.engine.common.virtualidentity.WorkflowIdentity(
        id = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[_root_.scala.Predef.String]
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = VirtualidentityProto.javaDescriptor.getMessageTypes().get(8)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = VirtualidentityProto.scalaDescriptor.messages(8)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = edu.uci.ics.amber.engine.common.virtualidentity.WorkflowIdentity(
    id = ""
  )
  implicit class WorkflowIdentityLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, edu.uci.ics.amber.engine.common.virtualidentity.WorkflowIdentity]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, edu.uci.ics.amber.engine.common.virtualidentity.WorkflowIdentity](_l) {
    def id: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.id)((c_, f_) => c_.copy(id = f_))
  }
  final val ID_FIELD_NUMBER = 1
  def of(
    id: _root_.scala.Predef.String
  ): _root_.edu.uci.ics.amber.engine.common.virtualidentity.WorkflowIdentity = _root_.edu.uci.ics.amber.engine.common.virtualidentity.WorkflowIdentity(
    id
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[edu.uci.ics.amber.engine.common.WorkflowIdentity])
}
