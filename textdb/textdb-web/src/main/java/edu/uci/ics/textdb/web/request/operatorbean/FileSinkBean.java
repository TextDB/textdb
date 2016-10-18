package edu.uci.ics.textdb.web.request.operatorbean;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.uci.ics.textdb.plangen.operatorbuilder.FileSinkBuilder;
import edu.uci.ics.textdb.web.request.OperatorBean;

import java.util.HashMap;

/**
 * This class defines the properties/data members specific to the FileSink operator
 * and extends the OperatorBean class which defines the data members general to all operators
 * Created by kishorenarendran on 10/17/16.
 */
public class FileSinkBean extends OperatorBean {
    @JsonProperty("file_path")
    private String filePath;

    public FileSinkBean() {
    }

    public FileSinkBean(String operatorID, String operatorType, String filePath) {
        super(operatorID, operatorType);
        this.filePath = filePath;
    }

    @JsonProperty("file_path")
    public String getFilePath() {
        return filePath;
    }

    @JsonProperty("file_path")
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public HashMap<String, String> getOperatorProperties() {
        HashMap<String, String> operatorProperties = super.getOperatorProperties();
        operatorProperties.put(FileSinkBuilder.FILE_PATH, this.getFilePath());
        return operatorProperties;
    }
}
