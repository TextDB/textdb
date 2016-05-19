package edu.uci.ics.textdb.dataflow.common;

import edu.uci.ics.textdb.api.common.Attribute;
import edu.uci.ics.textdb.api.common.IPredicate;
import edu.uci.ics.textdb.api.storage.IDataStore;
import edu.uci.ics.textdb.common.exception.DataFlowException;
import edu.uci.ics.textdb.common.utils.Utils;
import edu.uci.ics.textdb.storage.DataReaderPredicate;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import java.util.ArrayList;
import java.util.List;

/*
 * @author varun bharill, parag saraogi
 * 
 * This class builds the query to perform boolean searches in a lucene index. 
 */
public class FuzzyTokenPredicate implements IPredicate {
	
    private IDataStore dataStore;
    private String query;
    private Query luceneQuery;
    private ArrayList<String> tokens;
    private List<Attribute> attributeList;
    private String[] fields;
    private Analyzer luceneAnalyzer;
    private double thresholdRatio;
    private int threshold;
    private boolean isSpanInformationAdded;
    
    public FuzzyTokenPredicate(String query, List<Attribute> attributeList, Analyzer analyzer,IDataStore dataStore, double thresholdRatio, boolean isSpanInformationAdded) throws DataFlowException{
        try {
        	this.thresholdRatio = thresholdRatio;
        	this.dataStore = dataStore;
        	this.luceneAnalyzer = analyzer;
        	this.isSpanInformationAdded= isSpanInformationAdded;
            this.query = query;
            this.tokens = Utils.tokenizeQuery(analyzer, query);
            this.computeThreshold();
            this.attributeList = attributeList;
            this.extractSearchFields();
            this.luceneQuery = this.createLuceneQueryObject();
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataFlowException(e.getMessage(), e);
        }
    }
    
    private void extractSearchFields() {
    	this.fields = new String[this.attributeList.size()];
    	int i = 0;
    	for( Attribute a : this.attributeList) {
    		this.fields[i] = a.getFieldName();
    		i++;
    	}
	}

    /*
     * The input threshold given by the end-user (thresholdRatio data member) is a ratio
     * but boolean search query requires integer as a threshold.
     */
	public void computeThreshold() {
		this.threshold = (int) (this.thresholdRatio * this.tokens.size());
    	if(this.threshold == 0){
    		this.threshold = 1;
    	}
    }
	
    private Query createLuceneQueryObject() throws ParseException {
    	if(this.threshold > 1024)
    		BooleanQuery.setMaxClauseCount(this.threshold + 1);
    	BooleanQuery.Builder builder = new BooleanQuery.Builder();
    	builder.setMinimumNumberShouldMatch(this.threshold);
    	MultiFieldQueryParser qp = new MultiFieldQueryParser(fields, this.luceneAnalyzer);
    	for(String s : this.tokens) {
    		builder.add(qp.parse(s), Occur.SHOULD);
    	}
    	return builder.build();
    }

    public DataReaderPredicate getDataReaderPredicate() {
    	DataReaderPredicate dataReaderPredicate = new DataReaderPredicate(this.dataStore, this.luceneQuery,
                this.query, this.luceneAnalyzer, this.attributeList);
        return dataReaderPredicate;

    }


}
