package texera.operators.sleep;

import Engine.Common.AmberTuple.Tuple;
import Engine.Common.Constants;
import Engine.Operators.Common.Map.MapMetadata;
import Engine.Operators.OperatorMetadata;
import Engine.SchemaSupport.schema.Schema;
import scala.Function1;
import scala.Serializable;
import texera.common.workflow.TexeraOperator;
import texera.common.schema.TexeraOperatorDescription;
import texera.common.schema.OperatorGroupConstants;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class TexeraSleepOperator extends TexeraOperator {

    @JsonProperty("sleep")
    @JsonPropertyDescription("time to sleep for each tuple in milliseconds")
    public Integer sleepMilliseconds;

    @Override
    public OperatorMetadata amberOperator() {
        if (sleepMilliseconds == null) {
            sleepMilliseconds = 100;
        }
        return new MapMetadata(this.amberOperatorTag(), Constants.defaultNumWorkers(),
                (Function1<Tuple, Tuple> & Serializable) t -> {
                    try {
                        Thread.sleep(sleepMilliseconds);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return t;
                }) {

            Schema outputSchema = null;

            @Override
            public void setInputSchema(Schema schema) {
                outputSchema = schema;
            }

            @Override
            public Schema getOutputSchema() {
                return outputSchema;
            }
        };
    }

    @Override
    public TexeraOperatorDescription texeraOperatorDescription() {
        return new TexeraOperatorDescription(
                "Sleep", "do nothing but sleep for x miliseconds per tuple",
                OperatorGroupConstants.UTILITY_GROUP(),
                1, 1
        );
    }
}
