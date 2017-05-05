package edu.uci.ics.textdb.storage;

import java.io.File;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.uci.ics.textdb.api.exception.StorageException;
import edu.uci.ics.textdb.api.exception.TextDBException;
import edu.uci.ics.textdb.api.field.IDField;
import edu.uci.ics.textdb.api.field.StringField;
import edu.uci.ics.textdb.api.schema.Attribute;
import edu.uci.ics.textdb.api.schema.AttributeType;
import edu.uci.ics.textdb.api.schema.Schema;
import edu.uci.ics.textdb.api.tuple.Tuple;
import edu.uci.ics.textdb.api.utils.Utils;
import edu.uci.ics.textdb.storage.constants.LuceneAnalyzerConstants;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RelationManagerTest {
    
    RelationManager relationManager;
    
    @Before
    public void setUpRelationManager() throws TextDBException {
        relationManager = RelationManager.getRelationManager();
    }
    
    /*
     * Test the information about "table catalog" itself is stored properly.
     * 
     */
    @Test
    public void test1() throws Exception {
        String tableCatalogDirectory = 
                relationManager.getTableDirectory(CatalogConstants.TABLE_CATALOG);
        Analyzer tableCatalogLuceneAnalyzer = 
                relationManager.getTableAnalyzer(CatalogConstants.TABLE_CATALOG);
        Schema tableCatalogSchema = 
                relationManager.getTableSchema(CatalogConstants.TABLE_CATALOG);
                
        Assert.assertEquals(tableCatalogDirectory, 
                new File(CatalogConstants.TABLE_CATALOG_DIRECTORY).getCanonicalPath());
        Assert.assertTrue(tableCatalogLuceneAnalyzer instanceof StandardAnalyzer);
        Assert.assertEquals(tableCatalogSchema, Utils.getSchemaWithID(CatalogConstants.TABLE_CATALOG_SCHEMA));
    }
    
    /*
     * Test the information about "schema catalog" itself is stored properly.
     */
    @Test
    public void test2() throws Exception {
        String schemaCatalogDirectory = 
                relationManager.getTableDirectory(CatalogConstants.SCHEMA_CATALOG);
        Analyzer schemaCatalogLuceneAnalyzer = 
                relationManager.getTableAnalyzer(CatalogConstants.SCHEMA_CATALOG);
        Schema schemaCatalogSchema = 
                relationManager.getTableSchema(CatalogConstants.SCHEMA_CATALOG);
        
        Assert.assertEquals(schemaCatalogDirectory, 
                new File(CatalogConstants.SCHEMA_CATALOG_DIRECTORY).getCanonicalPath());
        Assert.assertTrue(schemaCatalogLuceneAnalyzer instanceof StandardAnalyzer);
        Assert.assertEquals(schemaCatalogSchema, Utils.getSchemaWithID(CatalogConstants.SCHEMA_CATALOG_SCHEMA));  
    }
    
    /*
     * Create a table and test if table's information can be retrieved successfully.
     */
    @Test
    public void test3() throws Exception {        
        String tableName = "relation_manager_test_table_1";
        String tableDirectory = "./index/test_table_1/";
        Schema tableSchema = new Schema(
                new Attribute("city", AttributeType.STRING),
                new Attribute("description", AttributeType.TEXT), new Attribute("tax rate", AttributeType.DOUBLE),
                new Attribute("population", AttributeType.INTEGER), new Attribute("record time", AttributeType.DATE));
        String tableLuceneAnalyzerString = LuceneAnalyzerConstants.standardAnalyzerString();
        Analyzer tableLuceneAnalyzer = LuceneAnalyzerConstants.getLuceneAnalyzer(tableLuceneAnalyzerString);
        
        RelationManager relationManager = RelationManager.getRelationManager();
        
        relationManager.deleteTable(tableName);
        
        relationManager.createTable(
                tableName, tableDirectory, tableSchema, tableLuceneAnalyzerString);
        
        Assert.assertEquals(new File(tableDirectory).getCanonicalPath(), 
                relationManager.getTableDirectory(tableName));
        Assert.assertEquals(Utils.getSchemaWithID(tableSchema), relationManager.getTableSchema(tableName));
        Assert.assertEquals(tableLuceneAnalyzer.getClass(), relationManager.getTableAnalyzer(tableName).getClass());
        
        relationManager.deleteTable(tableName);
    }
    
    /*
     * Retrieving the directory of a table which doesn't exist should result in an exception.
     */
    @Test(expected = StorageException.class)
    public void test4() throws Exception {
        String tableName = "relation_manager_test_table_1";
        RelationManager.getRelationManager().getTableDirectory(tableName);
    }
    
    /*
     * Retrieving the schema of a table which doesn't exist should result in an exception.
     */
    @Test(expected = StorageException.class)
    public void test5() throws Exception {
        String tableName = "relation_manager_test_table_1";
        RelationManager.getRelationManager().getTableSchema(tableName);
    }
    
    /*
     * Retrieving the lucene analyzer of a table which doesn't exist should result in an exception.
     */
    @Test(expected = StorageException.class)
    public void test6() throws Exception {
        String tableName = "relation_manager_test_table_1";
        RelationManager.getRelationManager().getTableAnalyzer(tableName);
    }
    

    /*
     * Test creating and deleting multiple tables in relation manager.
     */
    @Test
    public void test7() throws Exception {
        String tableName = "relation_manager_test_table";
        String tableDirectory = "./index/test_table";
        Schema tableSchema = new Schema(
                new Attribute("city", AttributeType.STRING),
                new Attribute("description", AttributeType.TEXT), new Attribute("tax rate", AttributeType.DOUBLE),
                new Attribute("population", AttributeType.INTEGER), new Attribute("record time", AttributeType.DATE));
        
        int NUM_OF_LOOPS = 10;
        RelationManager relationManager = RelationManager.getRelationManager();
        
        // create tables
        for (int i = 0; i < NUM_OF_LOOPS; i++) {
            // delete previously inserted tables first
            relationManager.deleteTable(
                    tableName + '_' + i);
            relationManager.createTable(
                    tableName + '_' + i,
                    tableDirectory + '_' + i, 
                    tableSchema, 
                    LuceneAnalyzerConstants.standardAnalyzerString());
        }
        // assert tables are correctly created
        for (int i = 0; i < NUM_OF_LOOPS; i++) {
            Assert.assertEquals(new File(tableDirectory + '_' + i).getCanonicalPath(), 
                    relationManager.getTableDirectory(tableName + '_' + i));
            Assert.assertEquals(Utils.getSchemaWithID(tableSchema), relationManager.getTableSchema(tableName + '_' + i));
        }
        // delete tables
        for (int i = 0; i < NUM_OF_LOOPS; i++) {
            relationManager.deleteTable(tableName + '_' + i);
        }
        // assert tables are correctly deleted
        int errorCount = 0;
        for (int i = 0; i < NUM_OF_LOOPS; i++) {
            try {
                relationManager.getTableDirectory(tableName + '_' + i);
            } catch (StorageException e) {
                errorCount++;
            }
        }
        Assert.assertEquals(NUM_OF_LOOPS, errorCount);
    }
    
    /*
     * Test inserting a tuple to a table and then delete it 
     */
    @Test
    public void test8() throws Exception {
        String tableName = "relation_manager_test_table";
        String tableDirectory = "./index/test_table";
        Schema tableSchema = new Schema(
                new Attribute("content", AttributeType.STRING));
        
        RelationManager relationManager = RelationManager.getRelationManager();
        
        relationManager.deleteTable(tableName);
        relationManager.createTable(
                tableName, tableDirectory, tableSchema, LuceneAnalyzerConstants.standardAnalyzerString());
        
        DataWriter dataWriter = relationManager.getTableDataWriter(tableName);
        
        dataWriter.open();
        Tuple insertedTuple = new Tuple(tableSchema, new StringField("test"));        
        IDField idField = dataWriter.insertTuple(insertedTuple);
        dataWriter.close();
                
        Tuple returnedTuple = relationManager.getTupleByID(tableName, idField);
        
        Assert.assertEquals(insertedTuple.getField("content").getValue().toString(), 
                returnedTuple.getField("content").getValue().toString());
        
        dataWriter.open();
        dataWriter.deleteTupleByID(idField);
        dataWriter.close();
        
        Tuple deletedTuple = relationManager.getTupleByID(tableName, idField);
        Assert.assertNull(deletedTuple);
        
        relationManager.deleteTable(tableName);
    }
    
    /*
     * Test inserting a tuple to a table, then update it, then delete it 
     */
    @Test
    public void test9() throws Exception {
        String tableName = "relation_manager_test_table";
        String tableDirectory = "./index/test_table";
        Schema tableSchema = new Schema(
                new Attribute("content", AttributeType.STRING));
        
        RelationManager relationManager = RelationManager.getRelationManager();
        
        relationManager.deleteTable(tableName);
        relationManager.createTable(
                tableName, tableDirectory, tableSchema, LuceneAnalyzerConstants.standardAnalyzerString());
        
        DataWriter dataWriter = relationManager.getTableDataWriter(tableName);
        
        dataWriter.open();
        Tuple insertedTuple = new Tuple(tableSchema, new StringField("test"));
        IDField idField = dataWriter.insertTuple(insertedTuple);
        dataWriter.close();
        
        Tuple returnedTuple = relationManager.getTupleByID(tableName, idField);
        
        Assert.assertEquals(insertedTuple.getField("content").getValue().toString(), 
                returnedTuple.getField("content").getValue().toString());
        
        dataWriter.open();
        Tuple updatedTuple = new Tuple(tableSchema, new StringField("testUpdate"));
        dataWriter.updateTuple(updatedTuple, idField);
        dataWriter.close();
        Tuple returnedUpdatedTuple = relationManager.getTupleByID(tableName, idField);
        
        Assert.assertEquals(updatedTuple.getField("content").getValue().toString(), 
                returnedUpdatedTuple.getField("content").getValue().toString());
        
        dataWriter.open();
        dataWriter.deleteTupleByID(idField);
        dataWriter.close();
        
        Tuple deletedTuple = relationManager.getTupleByID(tableName, idField);
        Assert.assertNull(deletedTuple);
        
        relationManager.deleteTable(tableName);   
    }
    
    
    /*
     * Test inserting multiple tuples to a table, getting them by a query, then deleting them by a query
     */
    @Test
    public void test10() throws Exception {
        String tableName = "relation_manager_test_table";
        String tableDirectory = "./index/test_table";
        Schema tableSchema = new Schema(
                new Attribute("content", AttributeType.STRING), new Attribute("number", AttributeType.STRING));
        
        RelationManager relationManager = RelationManager.getRelationManager();
        
        relationManager.deleteTable(tableName);
        relationManager.createTable(
                tableName, tableDirectory, tableSchema, LuceneAnalyzerConstants.standardAnalyzerString());
        
        DataWriter dataWriter = relationManager.getTableDataWriter(tableName);
        
        dataWriter.open();
        
        Tuple insertedTuple = new Tuple(tableSchema, new StringField("test"), new StringField("1"));
        dataWriter.insertTuple(insertedTuple);
                
        Tuple insertedTuple2 = new Tuple(tableSchema, new StringField("test"), new StringField("2"));
        IDField idField2 = dataWriter.insertTuple(insertedTuple2);
        
        Tuple insertedTuple3 = new Tuple(tableSchema, new StringField("test"), new StringField("3"));
        dataWriter.insertTuple(insertedTuple3);
        
        dataWriter.close();
        
        // test should match all 3 tuples
        Query allTupleQuery = new TermQuery(new Term("content", "test"));
        DataReader allTupleReader = relationManager.getTableDataReader(tableName, allTupleQuery);
        
        int tupleCounter = 0;
        allTupleReader.open();
        while (allTupleReader.getNextTuple() != null) {
            tupleCounter++;
        }
        allTupleReader.close();
        
        Assert.assertEquals(3, tupleCounter);
        
        // tuple 2 should be deleted
        Query tuple2Query = new TermQuery(new Term("number", "2"));
        
        dataWriter.open();
        dataWriter.deleteTuple(tuple2Query);
        dataWriter.close();
        
        Tuple deletedTuple = relationManager.getTupleByID(tableName, idField2);
        Assert.assertNull(deletedTuple);
        
        relationManager.deleteTable(tableName);       
    }
    
    /*
     * Test deleting "table catalog" table should fail.
     */
    @Test(expected = StorageException.class)
    public void test11() throws Exception {
        relationManager.deleteTable(CatalogConstants.TABLE_CATALOG);       
    }
    
    /*
     * Test deleting "schema catalog" table should fail.
     */
    @Test(expected = StorageException.class)
    public void test12() throws Exception {
        relationManager.deleteTable(CatalogConstants.SCHEMA_CATALOG);       
    }
    
    /*
     * Test trying to get the DataWriter for "table catalog" table should fail.
     */
    @Test(expected = StorageException.class)
    public void test13() throws Exception {
        relationManager.getTableDataWriter(CatalogConstants.TABLE_CATALOG);       
    }
    
    /*
     * Test trying to get the DataWriter for "schema catalog" table should fail.
     */
    @Test(expected = StorageException.class)
    public void test14() throws Exception {
        relationManager.getTableDataWriter(CatalogConstants.SCHEMA_CATALOG);           
    }
    
    @Test
    public void test15() throws Exception {
        String tableName1 = "relation_manager_test_table_15_1";
        String tableName2 = "relation_manager_test_table_15_2";
        
        String indexDirectory = "./index/test_table/relation_manager_test_table_15";
        Schema schema = new Schema(new Attribute("content", AttributeType.TEXT));
        String luceneAnalyzerString = "standard";
        
        relationManager.deleteTable(tableName1);
        relationManager.deleteTable(tableName2);
        
        relationManager.createTable(tableName1, indexDirectory, schema, luceneAnalyzerString);
        
        // create another table with the same directory should fail
        try {
            relationManager.createTable(tableName2, indexDirectory, schema, luceneAnalyzerString);
            System.out.println(relationManager.getTableDirectory(tableName1));
            System.out.println(relationManager.getTableDirectory(tableName2));
            Assert.fail("Storage exception should be thrown because of duplicate index directories");
        } catch (StorageException e) {
        }
        
        relationManager.deleteTable(tableName1);  
    }
    
    /*
     * Test that table name with upper case in it is handled properly.
     */
    @Test
    public void test16() throws Exception {
        String tableName1 = "Relation_Manager_Test_Table_15_1";
        
        String indexDirectory = "./index/test_table/relation_manager_test_table_16";
        Schema schema = new Schema(new Attribute("content", AttributeType.TEXT));
        String luceneAnalyzerString = "standard";
        
        relationManager.deleteTable(tableName1);
        
        relationManager.createTable(tableName1, indexDirectory, schema, luceneAnalyzerString);
        
        Assert.assertTrue(relationManager.checkTableExistence(tableName1));
        
        relationManager.deleteTable(tableName1);  
        
        Assert.assertTrue(! relationManager.checkTableExistence(tableName1));
    }

    /*
     * Test on getMetaData() to see if it successfully get metadata from "promed" and "plan"
     */
    @Test
    public void test17() throws Exception {
        System.out.println("\n");
        // Check if the result contains and only contains 2 tables with correct table names
        List<TableMetadata> result = relationManager.getMetaData();
//        System.out.println(result.get(1).getSchema().getAttributes().toString());

        HashSet<String> tableNamesActual = new HashSet<>();
        for(int i = 0; i < result.size(); ++i) {
            TableMetadata currentMetadata = result.get(i);
            tableNamesActual.add(currentMetadata.getTableName());

            List<Attribute> attributes = currentMetadata.getSchema().getAttributes();
            for(int j = 0; j < attributes.size(); ++j) {
                String attributeName = attributes.get(j).getAttributeName();
                if(attributeName.equals("_id")) {
                    Assert.assertEquals(AttributeType._ID_TYPE, attributes.get(j).getAttributeType());
                } else if(attributeName.equals("id") || attributeName.equals("name") ||
                        attributeName.equals("desc") || attributeName.equals("planJson")) {
                    Assert.assertEquals(AttributeType.STRING, attributes.get(j).getAttributeType());
                } else if(attributeName.equals("content")) {
                    Assert.assertEquals(AttributeType.TEXT, attributes.get(j).getAttributeType());
                }
            }
        }

        HashSet<String> tableNamesExpect = new HashSet<>();
        tableNamesExpect.add("promed");
        tableNamesExpect.add("plan");

        Assert.assertEquals(2, tableNamesActual.size());
        Assert.assertEquals(tableNamesExpect, tableNamesActual);

    }

}
