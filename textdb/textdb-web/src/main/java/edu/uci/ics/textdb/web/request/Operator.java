package edu.uci.ics.textdb.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.uci.ics.textdb.web.request.operatorbean.RegexMatcherBean;

/**
 * This class is the abstract class that defines the data members common to all operators. It is
 * extended by invidual operator beans in order to define the data members specific to each
 * operator
 * Created by kishorenarendran on 10/12/16.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="operator_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value=RegexMatcherBean.class, name="RegexMatcher"),
})
public abstract class Operator {
    @JsonProperty("operator_id")
    private String operatorID;
    @JsonProperty("operator_type")
    private String operatorType;

    public Operator() {
    }

    public Operator(String operatorID, String operatorType) {
        this.operatorID = operatorID;
        this.operatorType = operatorType;
    }

    @JsonProperty("operator_id")
    public String getOperatorID() {
        return operatorID;
    }

    @JsonProperty("operator_id")
    public void setOperatorID(String operatorID) {
        this.operatorID = operatorID;
    }

    @JsonProperty("operator_type")
    public String getOperatorType() {
        return operatorType;
    }

    @JsonProperty("operator_type")
    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }
}
