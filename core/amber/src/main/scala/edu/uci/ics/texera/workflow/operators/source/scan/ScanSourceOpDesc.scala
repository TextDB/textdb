package edu.uci.ics.texera.workflow.operators.source.scan
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle
import edu.uci.ics.texera.web.resource.dashboard.file.UserFileUtils
import edu.uci.ics.texera.workflow.common.operators.source.SourceOperatorDescriptor
import edu.uci.ics.texera.workflow.common.WorkflowContext
import edu.uci.ics.texera.workflow.common.metadata.{
  OperatorGroupConstants,
  OperatorInfo,
  OutputPort
}
import edu.uci.ics.texera.workflow.common.tuple.schema.Schema
import org.codehaus.jackson.map.annotate.JsonDeserialize

import java.util.Collections.singletonList
import scala.collection.JavaConverters.asScalaBuffer
import scala.collection.immutable.List

abstract class ScanSourceOpDesc extends SourceOperatorDescriptor {

  /** in the case we do not want to read the entire large file, but only
    * the first a few lines of it to do the type inference.
    */
  @JsonIgnore
  var INFER_READ_LIMIT: Int = 100

  @JsonProperty(required = true)
  @JsonSchemaTitle("File")
  @JsonDeserialize(contentAs = classOf[java.lang.String])
  var fileName: Option[String] = None

  @JsonIgnore
  var filePath: Option[String] = None

  @JsonIgnore
  var fileTypeName: Option[String] = None

  override def sourceSchema(): Schema = {
    if (filePath.isEmpty) return null
    inferSchema()

  }

  override def setContext(workflowContext: WorkflowContext): Unit = {
    super.setContext(workflowContext)

    if (context.userID.isDefined)
      // if context has a valid user ID, the fileName will be a file name,
      // resolve fileName to be the actual file path.
      filePath = Some(
        UserFileUtils.getFilePath(context.userID.get.toString, fileName.get).toString
      )
    else
      // otherwise, the fileName will be inputted by user, which is the filePath.
      filePath = fileName

  }

  override def operatorInfo: OperatorInfo = {
    OperatorInfo(
      userFriendlyName = s"${fileTypeName.get} File Scan",
      operatorDescription = s"Scan data from a ${fileTypeName.get} file",
      OperatorGroupConstants.SOURCE_GROUP,
      List.empty,
      asScalaBuffer(singletonList(OutputPort(""))).toList
    )
  }

  def inferSchema(): Schema
}
