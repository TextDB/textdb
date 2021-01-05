package edu.uci.ics.texera.workflow.operators.projection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.base.Preconditions;
import edu.uci.ics.texera.workflow.common.metadata.OperatorGroupConstants;
import edu.uci.ics.texera.workflow.common.metadata.OperatorInfo;
import edu.uci.ics.texera.workflow.common.operators.OneToOneOpExecConfig;
import edu.uci.ics.texera.workflow.common.operators.map.MapOpDesc;
import edu.uci.ics.texera.workflow.common.tuple.schema.Schema;

import java.util.List;
import java.util.stream.Collectors;

public class ProjectionOpDesc extends MapOpDesc {
    @JsonProperty(value = "attributes", required = true)
    @JsonPropertyDescription("a subset of column to keeps")
    public List<String> attributes;

    @Override
    public OneToOneOpExecConfig operatorExecutor() {
        if (attributes == null) {
            throw new RuntimeException("Projection: attribute is null");
        }
        return new OneToOneOpExecConfig(operatorIdentifier(), i -> new ProjectionOpExec(this));
    }

    @Override
    public OperatorInfo operatorInfo() {
        return new OperatorInfo(
                "Projection",
                "keeps the column",
                OperatorGroupConstants.UTILITY_GROUP(),
                1, 1, false
        );
    }

    @Override
    public Schema getOutputSchema(Schema[] schemas) {
        Preconditions.checkArgument(schemas.length == 1);
        List<String> attributesToRemove = schemas[0].getAttributeNames().stream()
                .filter(item -> !this.attributes.contains(item))
                .collect(Collectors.toList());
        return Schema.newBuilder().add(schemas[0]).removeIfExists(attributesToRemove).build();
    }
}
