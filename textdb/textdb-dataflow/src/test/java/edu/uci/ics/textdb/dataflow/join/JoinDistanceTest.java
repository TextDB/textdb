package edu.uci.ics.textdb.dataflow.join;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.uci.ics.textdb.api.common.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.uci.ics.textdb.api.common.AttributeType;
import edu.uci.ics.textdb.api.exception.TextDBException;
import edu.uci.ics.textdb.common.constants.LuceneAnalyzerConstants;
import edu.uci.ics.textdb.common.constants.DataConstants.KeywordMatchingType;
import edu.uci.ics.textdb.common.exception.DataFlowException;
import edu.uci.ics.textdb.common.field.DataTuple;
import edu.uci.ics.textdb.common.field.IntegerField;
import edu.uci.ics.textdb.common.field.ListField;
import edu.uci.ics.textdb.common.field.Span;
import edu.uci.ics.textdb.common.field.StringField;
import edu.uci.ics.textdb.common.field.TextField;
import edu.uci.ics.textdb.common.utils.Utils;
import edu.uci.ics.textdb.dataflow.common.FuzzyTokenPredicate;
import edu.uci.ics.textdb.dataflow.common.JoinDistancePredicate;
import edu.uci.ics.textdb.dataflow.fuzzytokenmatcher.FuzzyTokenMatcherSourceOperator;
import edu.uci.ics.textdb.dataflow.keywordmatch.KeywordMatcherSourceOperator;
import edu.uci.ics.textdb.dataflow.projection.ProjectionOperator;
import edu.uci.ics.textdb.dataflow.projection.ProjectionPredicate;
import edu.uci.ics.textdb.dataflow.utils.TestUtils;
import junit.framework.Assert;

/**
 * JoinDistanceTest tests the Join Operator and the JoinDistancePredicate.
 * 
 * The test classes are organized as the following:
 *   JoinDistanceTest   includes (only) the main test logic.
 *   JoinDistanceHelper includes helper functions and wrapper functions.
 *   JoinTestConstants  includes the schema and data that Join tests use.
 * 
 * @author sripadks
 * @author Zuozhi Wang
 *
 */
public class JoinDistanceTest {
    
    public static final String BOOK_TABLE= JoinTestHelper.BOOK_TABLE;
    
    public static final KeywordMatchingType conjunction = KeywordMatchingType.CONJUNCTION_INDEXBASED;
    public static final KeywordMatchingType phrase = KeywordMatchingType.PHRASE_INDEXBASED;

    
    /*
     * The annotators @BeforeClass and @AfterClass are used instead of @Before and @After.
     * 
     * The difference is that:
     *   @Before and @After are executed before and after EACH test case.
     *   @BeforeClass and @AfterClass are executed once before ALL the test begin and ALL the test have finished.
     * 
     * We don't want to create and delete the tables on every test case, 
     *   therefore BeforeClass and AfterClass are better options.
     *   
     */
    @BeforeClass
    public static void setup() throws TextDBException {
        // writes the test tables before ALL tests
        JoinTestHelper.createTestTables();
    }
    
    @AfterClass
    public static void cleanUp() throws TextDBException {
        // deletes the test tables after ALL tests
        JoinTestHelper.deleteTestTables();
    }
    
    /*
     * The @After annotation is used here because we want to clear the data in two tables after EVERY test case. 
     * This is because different test cases write different data in to the test tables, 
     *   and they need to be cleared before the next test case begins.
     */
    @After
    public void clearTables() throws TextDBException {
        // clear the test tables after EACH test
        JoinTestHelper.clearTestTables();
    }
    

    /*
     * This case tests for the scenario when the difference of keyword spans
     *  is within the given span threshold.
     *  e.g.
     *  [<11, 18>]
     *  [<27, 33>]
     *  threshold = 20 (within threshold)
     *  result: [<11, 33>]
     *  
     * Test result: The list contains a tuple with all the fields and a span
     * list consisting of the joined span. The joined span is made up of the
     * field name, start and stop index (computed as <min(span1 spanStartIndex,
     * span2 spanStartIndex), max(span1 spanEndIndex, span2 spanEndIndex)>)
     * key (combination of span1 key and span2 key) and value (combination of
     * span1 value and span2 value).
     * 
     */
    @Test
    public void testIdsMatchFieldsMatchSpanWithinThreshold() throws Exception {
        JoinTestHelper.insertToTable(BOOK_TABLE, JoinTestConstants.bookGroup1.get(0));
                
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "special", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "writer", conjunction);
        
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 20), Integer.MAX_VALUE, 0);
        
        Schema resultSchema = Utils.createSpanSchema(JoinTestConstants.BOOK_SCHEMA);
        List<Span> spanList = new ArrayList<>();

        Span span1 = new Span(JoinTestConstants.REVIEW, 11, 33, "special_writer", "special kind of " + "writer");
        spanList.add(span1);

        IField[] book1 = { new IntegerField(52), new StringField("Mary Roach"),
                new StringField("Grunt: The Curious Science of Humans at War"), new IntegerField(288),
                new TextField("It takes a special kind " + "of writer to make topics ranging from death to our "
                        + "gastrointestinal tract interesting (sometimes "
                        + "hilariously so), and pop science writer Mary Roach is " + "always up to the task."),
                new ListField<>(spanList) };
        
        ITuple expectedTuple = new DataTuple(resultSchema, book1);
        List<ITuple> expectedResult = new ArrayList<>();
        expectedResult.add(expectedTuple);

        Assert.assertEquals(1, resultList.size());
        Assert.assertTrue(TestUtils.equals(expectedResult, resultList));
    }
    
    /*
     * This case tests for the scenario when the difference of keyword spans
     * to be joined is greater than the threshold.
     * 
     * [<11, 18>]
     * [<42, 48>]
     * threshold = 20 (beyond threshold)
     * 
     * Test result: An empty list is returned.
     */
    @Test
    public void testIdsMatchFieldsMatchSpanExceedThreshold() throws Exception {
        JoinTestHelper.insertToTable(BOOK_TABLE, JoinTestConstants.bookGroup1.get(0));
                
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "special", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "topics", conjunction);
        
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 20), Integer.MAX_VALUE, 0);

        Assert.assertEquals(0, resultList.size());
    }
    
    // This case tests for the scenario when either/both of the operators'
    // result lists are empty (i.e. when one/both of the operators' are
    // not able to find any suitable matches)
    // Test result: Join should return an empty list.
    @Test
    public void testOneOfTheOperatorResultIsEmpty() throws Exception {
        JoinTestHelper.insertToTable(BOOK_TABLE, JoinTestConstants.bookGroup1.get(0));
        
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "special", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "book", conjunction);
 
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 20), Integer.MAX_VALUE, 0);
        
        Assert.assertEquals(0, resultList.size());
    }
    
    // This case tests for the scenario when one of the operators result lists has no span. 
    // If one of the operators doesn't have span, then an exception will be thrown.
    // Test result: DataFlowException is thrown
    @Test(expected = DataFlowException.class)
    public void testOneOfTheOperatorResultContainsNoSpan() throws Exception {
        JoinTestHelper.insertToTable(BOOK_TABLE, JoinTestConstants.bookGroup1.get(0));
        
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "special", conjunction);

        String fuzzyTokenQuery = "this writer writes well";
        double thresholdRatio = 0.25;
        List<String> textAttributeNames = JoinTestConstants.BOOK_SCHEMA.getAttributes().stream()
                .filter(attr -> attr.getAttributeType() != AttributeType.TEXT)
                .map(Attribute::getAttributeName).collect(Collectors.toList());
        FuzzyTokenPredicate fuzzyPredicateInner = new FuzzyTokenPredicate(fuzzyTokenQuery, textAttributeNames,
                LuceneAnalyzerConstants.getStandardAnalyzer(), thresholdRatio);
        FuzzyTokenMatcherSourceOperator fuzzyMatcherInner = new FuzzyTokenMatcherSourceOperator(fuzzyPredicateInner, BOOK_TABLE);
        
        ProjectionPredicate removeSpanListPredicate = new ProjectionPredicate(
                JoinTestConstants.BOOK_SCHEMA.getAttributeNames());
        ProjectionOperator removeSpanListProjection = new ProjectionOperator(removeSpanListPredicate);
        removeSpanListProjection.setInputOperator(fuzzyMatcherInner);
        
        JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, removeSpanListProjection, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 20), Integer.MAX_VALUE, 0);       
    }
    
    // This case tests for the scenario when one of the spans to be joined encompasses the other span
    // and both |(span 1 spanStartIndex) - (span 2 spanStartIndex)|,
    // |(span 1 spanEndIndex) - (span 2 spanEndIndex)| are within threshold.
    // e.g.
    // [<11, 18>]
    // [<3, 33>]
    // threshold = 20 (within threshold)
    // Test result: The bigger span should be returned.
    // [<3, 33>]
    @Test
    public void testOneSpanEncompassesOtherAndDifferenceLessThanThreshold() throws Exception {
        JoinTestHelper.insertToTable(BOOK_TABLE, JoinTestConstants.bookGroup1.get(0));
        
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "special", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "takes a special kind of writer", phrase);
 
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 20), Integer.MAX_VALUE, 0);
        
        
        Schema resultSchema = Utils.createSpanSchema(JoinTestConstants.BOOK_SCHEMA);
        List<Span> spanList = new ArrayList<>();

        Span span1 = new Span(JoinTestConstants.REVIEW, 3, 33, "special_takes a special " + "kind of writer",
                "takes a special " + "kind of writer");
        spanList.add(span1);

        IField[] book1 = { new IntegerField(52), new StringField("Mary Roach"),
                new StringField("Grunt: The Curious Science of Humans at War"), new IntegerField(288),
                new TextField("It takes a special kind " + "of writer to make topics ranging from death to our "
                        + "gastrointestinal tract interesting (sometimes "
                        + "hilariously so), and pop science writer Mary Roach is " + "always up to the task."),
                new ListField<>(spanList) };
        
        ITuple expectedTuple = new DataTuple(resultSchema, book1);
        List<ITuple> expectedResult = new ArrayList<>();
        expectedResult.add(expectedTuple);

        Assert.assertEquals(1, resultList.size());
        Assert.assertTrue(TestUtils.equals(expectedResult, resultList));
    }

    /*
     * This case tests for the scenario when one of the spans to be joined encompasses the other span
     * and |(span 1 spanStartIndex) - (span 2 spanStartIndex)|
     * and/or |(span 1 spanEndIndex) - (span 2 spanEndIndex)| exceed threshold.
     * 
     * e.g.
     * [<11, 18>]
     * [<3, 33>]
     * threshold = 10 (beyond threshold)
     * Test result: Join should return an empty list.
     */
    @Test
    public void testOneSpanEncompassesOtherAndDifferenceGreaterThanThreshold() throws Exception {
        JoinTestHelper.insertToTable(BOOK_TABLE, JoinTestConstants.bookGroup1.get(0));
        
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "special", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "takes a special kind of writer", phrase);
 
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 10), Integer.MAX_VALUE, 0);
        
        Assert.assertEquals(0, resultList.size());
    }
    
    /*
     * This case tests for the spans to be joined have some overlap and both
     * |(span 1 spanStartIndex) - (span 2 spanStartIndex)|,
     * |(span 1 spanEndIndex) - (span 2 spanEndIndex)| are within threshold.
     * 
     * e.g.
     * [<75, 97>]
     * [<92, 109>]
     * threshold = 20 (within threshold)
     * result: [<75, 109>]
     * 
     * Test result: The list contains a tuple with all the fields and a span
     * list consisting of the joined span. The joined span is made up of the
     * field name, start and stop index (computed as <min(span1 spanStartIndex,
     * span2 spanStartIndex), max(span1 spanEndIndex, span2 spanEndIndex)>)
     * key (combination of span1 key and span2 key) and value (combination of 
     * span1 value and span2 value).
     */
    @Test
    public void testSpansOverlapAndWithinThreshold() throws Exception {
        JoinTestHelper.insertToTable(BOOK_TABLE, JoinTestConstants.bookGroup1.get(0));
        
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "gastrointestinal tract", phrase);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "tract interesting", phrase);
 
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 20), Integer.MAX_VALUE, 0);
        
        Schema resultSchema = Utils.createSpanSchema(JoinTestConstants.BOOK_SCHEMA);
        List<Span> spanList = new ArrayList<>();

        Span span1 = new Span(JoinTestConstants.REVIEW, 75, 109, "gastrointestinal tract_" + "tract interesting",
                "gastrointestinal " + "tract interesting");
        spanList.add(span1);

        IField[] book1 = { new IntegerField(52), new StringField("Mary Roach"),
                new StringField("Grunt: The Curious Science of Humans at War"), new IntegerField(288),
                new TextField("It takes a special kind " + "of writer to make topics ranging from death to our "
                        + "gastrointestinal tract interesting (sometimes "
                        + "hilariously so), and pop science writer Mary Roach is " + "always up to the task."),
                new ListField<>(spanList) };
        ITuple expectedTuple = new DataTuple(resultSchema, book1);
        List<ITuple> expectedResult = new ArrayList<>();
        expectedResult.add(expectedTuple);

        Assert.assertEquals(1, resultList.size());
        Assert.assertTrue(TestUtils.equals(expectedResult, resultList));
    }
    
    // This case tests for the scenario when the spans to be joined have some overlap and
    // |(span 1 spanStartIndex) - (span 2 spanStartIndex)| and/or
    // |(span 1 spanEndIndex) - (span 2 spanEndIndex)| exceed threshold.
    // e.g.
    // [<75, 97>]
    // [<92, 109>]
    // threshold = 10
    // [    ]
    //    [        ]
    // <--> <-----> (beyond threshold)
    // Test result: Join should return an empty list.
    @Test
    public void testSpansOverlapAndExceedThreshold() throws Exception {
        JoinTestHelper.insertToTable(BOOK_TABLE, JoinTestConstants.bookGroup1.get(0));
        
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "takes a special", phrase);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "special kind of writer", phrase);
 
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 10), Integer.MAX_VALUE, 0);

        Assert.assertEquals(0, resultList.size());
    }
    
    // This case tests for the scenario when the spans to be joined are the same, i.e. both the keywords
    // are same.
    // e.g.
    // [<11, 18>]
    // [<11, 18>]
    // threshold = 20 (can be any non-negative number)
    // [ ]
    // [ ]
    // Test result: Join should return same span and key and the value in span
    // should be the same.
    // [<11, 18>]
    @Test
    public void testBothTheSpansAreSame() throws Exception {
        JoinTestHelper.insertToTable(BOOK_TABLE, JoinTestConstants.bookGroup1.get(0));
        
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "special", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "special", conjunction);
 
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 20), Integer.MAX_VALUE, 0);
        
        Schema resultSchema = Utils.createSpanSchema(JoinTestConstants.BOOK_SCHEMA);
        List<Span> spanList = new ArrayList<>();

        Span span1 = new Span(JoinTestConstants.REVIEW, 11, 18, "special_special", "special");
        spanList.add(span1);

        IField[] book1 = { new IntegerField(52), new StringField("Mary Roach"),
                new StringField("Grunt: The Curious Science of Humans at War"), new IntegerField(288),
                new TextField("It takes a special kind " + "of writer to make topics ranging from death to our "
                        + "gastrointestinal tract interesting (sometimes "
                        + "hilariously so), and pop science writer Mary Roach is " + "always up to the task."),
                new ListField<>(spanList) };
        ITuple expectedTuple = new DataTuple(resultSchema, book1);
        List<ITuple> expectedResult = new ArrayList<>();
        expectedResult.add(expectedTuple);

        Assert.assertEquals(1, resultList.size());
        Assert.assertTrue(TestUtils.equals(expectedResult, resultList));
    }
        
    // --------------------<END of single tuple test cases>--------------------

    
    /*
     * This case tests for the scenario when both the operators' have multiple
     * tuples and spans are within threshold.
     * 
     * Test result: Join should result in a list containing tuples with spans.
     * The number of tuples is equal to the number of tuples with spans within threshold.
     */
    @Test
    public void testBothOperatorsMultipleTuplesSpanWithinThreshold() throws Exception {
        List<ITuple> tuples = new ArrayList<>();
        tuples.add(JoinTestConstants.bookGroup1.get(3));
        tuples.add(JoinTestConstants.bookGroup2.get(2));
        tuples.add(JoinTestConstants.bookGroup2.get(4));
        
        JoinTestHelper.insertToTable(BOOK_TABLE, tuples);
           
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "review", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "book", conjunction);
        
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 12), Integer.MAX_VALUE, 0);

        Schema resultSchema = Utils.createSpanSchema(JoinTestConstants.BOOK_SCHEMA);
        List<Span> spanList = new ArrayList<>();

        Span span1 = new Span(JoinTestConstants.REVIEW, 0, 16, "review_book", "Review of a " + "Book");
        spanList.add(span1);
        Span span2 = new Span(JoinTestConstants.REVIEW, 62, 73, "review_book", "book review");
        spanList.add(span2);
        Span span3 = new Span(JoinTestConstants.REVIEW, 235, 246, "review_book", "book review");
        spanList.add(span3);

        IField[] book1 = { new IntegerField(54), new StringField("Andria Williams"),
                new StringField("The Longest Night: A Novel"), new IntegerField(400),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };

        IField[] book2 = { new IntegerField(65), new StringField("Sharon Guskin"),
                new StringField("The Forgetting Time: A Novel"), new IntegerField(368),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };

        IField[] book3 = { new IntegerField(63), new StringField("Paul Kalanithi"),
                new StringField("When Breath Becomes Air"), new IntegerField(256),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };

        ITuple expectedTuple1 = new DataTuple(resultSchema, book1);
        ITuple expectedTuple2 = new DataTuple(resultSchema, book2);
        ITuple expectedTuple3 = new DataTuple(resultSchema, book3);
        List<ITuple> expectedResult = new ArrayList<>();
        expectedResult.add(expectedTuple1);
        expectedResult.add(expectedTuple2);
        expectedResult.add(expectedTuple3);
        
        Assert.assertEquals(3, resultList.size());
        Assert.assertTrue(TestUtils.equals(expectedResult, resultList));
    }
    
    
    /*
     * This case tests for the scenario when both the operators' have multiple
     * tuples and none of spans are within threshold.
     * 
     * Test result: Join should result in an empty list.
     */
    @Test
    public void testBothOperatorsMultipleTuplesSpanExceedThreshold() throws Exception {
        List<ITuple> tuples = new ArrayList<>();
        tuples.add(JoinTestConstants.bookGroup1.get(3));
        tuples.add(JoinTestConstants.bookGroup2.get(2));
        tuples.add(JoinTestConstants.bookGroup2.get(4));
        
        JoinTestHelper.insertToTable(BOOK_TABLE, tuples);
           
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "review", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "book", conjunction);
        
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 4), Integer.MAX_VALUE, 0);
        Assert.assertEquals(0, resultList.size());
    }

    /*
     * This case tests for the scenario when the query has results over multiple
     * fields and join has to be performed only on the field mentioned in the attribute.
     * 
     * Test result: Join should return only those tuples which satisfy all the constraints.
     */
    @Test
    public void testQueryHasResultsOverMultipleFields() throws Exception {
        List<ITuple> tuples = JoinTestConstants.bookGroup1.subList(1, 5);
        
        JoinTestHelper.insertToTable(BOOK_TABLE, tuples);
           
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "typical", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "actually", conjunction);
        
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 90), Integer.MAX_VALUE, 0);

        Schema resultSchema = Utils.createSpanSchema(JoinTestConstants.BOOK_SCHEMA);
        List<Span> spanList = new ArrayList<>();

        Span span1 = new Span(JoinTestConstants.REVIEW, 28, 119, "typical_actually", "typical review. "
                + "This is a test. A book review test. " + "A test to test queries without actually");
        spanList.add(span1);
        Span span2 = new Span(JoinTestConstants.REVIEW, 186, 234, "typical_actually",
                "actually a review " + "even if it is not your typical");
        spanList.add(span2);
        
        IField[] book1 = { new IntegerField(51), new StringField("author unknown"), new StringField("typical"),
                new IntegerField(300),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };

        IField[] book2 = { new IntegerField(53), new StringField("Noah Hawley"), new StringField("Before the Fall"),
                new IntegerField(400),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };

        IField[] book3 = { new IntegerField(54), new StringField("Andria Williams"),
                new StringField("The Longest Night: A Novel"), new IntegerField(400),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };

        IField[] book4 = { new IntegerField(55), new StringField("Matti Friedman"),
                new StringField("Pumpkinflowers: A Soldier's " + "Story"), new IntegerField(256),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };

        ITuple expectedTuple1 = new DataTuple(resultSchema, book1);
        ITuple expectedTuple2 = new DataTuple(resultSchema, book2);
        ITuple expectedTuple3 = new DataTuple(resultSchema, book3);
        ITuple expectedTuple4 = new DataTuple(resultSchema, book4);
        List<ITuple> expectedResult = new ArrayList<>();
        expectedResult.add(expectedTuple1);
        expectedResult.add(expectedTuple2);
        expectedResult.add(expectedTuple3);
        expectedResult.add(expectedTuple4);
        
        Assert.assertEquals(4, resultList.size());
        Assert.assertTrue(TestUtils.equals(expectedResult, resultList));
    }
    
    // ---------------------<Limit and offset test cases.>---------------------

    /*
     * This case tests for the scenario when limit is some integer greater than
     * 0 and less than the actual number of results and offset is 0 and join 
     * is performed.
     * Test result: A list of tuples with number of tuples equal to limit.
     */
    @Test
    public void testForLimitWhenLimitIsLesserThanActualNumberOfResults() throws Exception{
        List<ITuple> tuples = JoinTestConstants.bookGroup1.subList(1, 5);
        
        JoinTestHelper.insertToTable(BOOK_TABLE, tuples);
           
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "typical", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "actually", conjunction);
        
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 90), 3, 0);

        Schema resultSchema = Utils.createSpanSchema(JoinTestConstants.BOOK_SCHEMA);
        List<Span> spanList = new ArrayList<>();
        
        Span span1 = new Span(JoinTestConstants.REVIEW, 28, 119, "typical_actually", "typical review. "
                + "This is a test. A book review test. " + "A test to test queries without actually");
        spanList.add(span1);
        Span span2 = new Span(JoinTestConstants.REVIEW, 186, 234, "typical_actually",
                "actually a review " + "even if it is not your typical");
        spanList.add(span2);

        IField[] book1 = { new IntegerField(51), new StringField("author unknown"), new StringField("typical"),
                new IntegerField(300),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };
        
        IField[] book2 = { new IntegerField(53), new StringField("Noah Hawley"),
                new StringField("Before the Fall"), new IntegerField(400),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };

        IField[] book3 = { new IntegerField(54), new StringField("Andria Williams"),
                new StringField("The Longest Night: A Novel"), new IntegerField(400),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };
        
        IField[] book4 = { new IntegerField(55), new StringField("Matti Friedman"),
                new StringField("Pumpkinflowers: A Soldier's " + "Story"), new IntegerField(256),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };

        ITuple expectedTuple1 = new DataTuple(resultSchema, book1);
        ITuple expectedTuple2 = new DataTuple(resultSchema, book2);
        ITuple expectedTuple3 = new DataTuple(resultSchema, book3);
        ITuple expectedTuple4 = new DataTuple(resultSchema, book4);
        List<ITuple> expectedResult = new ArrayList<>(3);
        expectedResult.add(expectedTuple1);
        expectedResult.add(expectedTuple2);
        expectedResult.add(expectedTuple3);
        expectedResult.add(expectedTuple4);
        
        Assert.assertEquals(3, resultList.size());
        Assert.assertTrue(TestUtils.containsAll(expectedResult, resultList));
    }

    /*
     * This case tests for the scenario when limit is some integer greater than
     * 0 and greater than the actual number of results and offset is 0 and join
     * is performed.
     * Test result: A list of tuples with number of tuples equal to the maximum
     * number of tuples operator can generate (which is lesser than limit.)
     */
    @Test
    public void testForLimitWhenLimitIsGreaterThanActualNumberOfResults() throws Exception{
        List<ITuple> tuples = JoinTestConstants.bookGroup1.subList(1, 5);
        
        JoinTestHelper.insertToTable(BOOK_TABLE, tuples);
           
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "typical", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "actually", conjunction);
        
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 90), 10, 0);

        Schema resultSchema = Utils.createSpanSchema(JoinTestConstants.BOOK_SCHEMA);
        List<Span> spanList = new ArrayList<>();

        Span span1 = new Span(JoinTestConstants.REVIEW, 28, 119, "typical_actually", "typical review. "
                + "This is a test. A book review test. " + "A test to test queries without actually");
        spanList.add(span1);
        Span span2 = new Span(JoinTestConstants.REVIEW, 186, 234, "typical_actually",
                "actually a review " + "even if it is not your typical");
        spanList.add(span2);

        IField[] book1 = { new IntegerField(51), new StringField("author unknown"), new StringField("typical"),
                new IntegerField(300),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };

        IField[] book2 = { new IntegerField(53), new StringField("Noah Hawley"), new StringField("Before the Fall"),
                new IntegerField(400),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };

        IField[] book3 = { new IntegerField(54), new StringField("Andria Williams"),
                new StringField("The Longest Night: A Novel"), new IntegerField(400),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };

        IField[] book4 = { new IntegerField(55), new StringField("Matti Friedman"),
                new StringField("Pumpkinflowers: A Soldier's " + "Story"), new IntegerField(256),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };

        ITuple expectedTuple1 = new DataTuple(resultSchema, book1);
        ITuple expectedTuple2 = new DataTuple(resultSchema, book2);
        ITuple expectedTuple3 = new DataTuple(resultSchema, book3);
        ITuple expectedTuple4 = new DataTuple(resultSchema, book4);
        List<ITuple> expectedResult = new ArrayList<>(5);
        expectedResult.add(expectedTuple1);
        expectedResult.add(expectedTuple2);
        expectedResult.add(expectedTuple3);
        expectedResult.add(expectedTuple4);

        Assert.assertEquals(4, resultList.size());
        Assert.assertTrue(TestUtils.equals(expectedResult, resultList));
    }

    /*
     * This case tests for the scenario when limit is 0 and offset is 0 and 
     * join is performed.
     * Test result: An empty list.
     */
    @Test
    public void testForLimitWhenLimitIsZero() throws Exception{
        List<ITuple> tuples = JoinTestConstants.bookGroup1.subList(1, 5);
        
        JoinTestHelper.insertToTable(BOOK_TABLE, tuples);
           
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "typical", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "actually", conjunction);
        
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 90), 0, 0);

        Assert.assertEquals(0, resultList.size());
    }

    /*
     * This case tests for the scenario when limit is 0 and offset is some 
     * integer greater than 0 and less than the actual number of results and 
     * join is performed.
     * Test result: An empty list.
     */
    @Test
    public void testForLimitWhenLimitIsZeroAndHasOffset() throws Exception{
        List<ITuple> tuples = JoinTestConstants.bookGroup1.subList(1, 5);
        
        JoinTestHelper.insertToTable(BOOK_TABLE, tuples);
           
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "typical", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "actually", conjunction);
        
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 90), 0, 2);

        Assert.assertEquals(0, resultList.size());
    }

    /*
     * This case tests for the scenario when limit is some integer greater than
     * 0 and less than the actual number of results and offset is some integer
     * greater than 0 and less than actual number of results and join is 
     * performed.
     * Test result: A list of tuples with number of tuples equal to limit 
     * starting from the set offset.
     */
    @Test
    public void testForLimitWhenLimitIsLesserThanActualNumberOfResultsAndHasOffset() throws Exception {
        List<ITuple> tuples = JoinTestConstants.bookGroup1.subList(1, 5);
        
        JoinTestHelper.insertToTable(BOOK_TABLE, tuples);
           
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "typical", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "actually", conjunction);
        
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 90), 1, 2);

        Schema resultSchema = Utils.createSpanSchema(JoinTestConstants.BOOK_SCHEMA);
        List<Span> spanList = new ArrayList<>();

        Span span1 = new Span(JoinTestConstants.REVIEW, 28, 119, "typical_actually", "typical review. "
                + "This is a test. A book review test. " + "A test to test queries without actually");
        spanList.add(span1);
        Span span2 = new Span(JoinTestConstants.REVIEW, 186, 234, "typical_actually",
                "actually a review " + "even if it is not your typical");
        spanList.add(span2);

        IField[] book1 = { new IntegerField(54), new StringField("Andria Williams"),
                new StringField("The Longest Night: A Novel"), new IntegerField(400),
                new TextField("Review of a Book. This is a typical " + "review. This is a test. A book review "
                        + "test. A test to test queries without " + "actually using actual review. From "
                        + "here onwards, we can pretend this to " + "be actually a review even if it is not "
                        + "your typical book review."),
                new ListField<>(spanList) };

        ITuple expectedTuple1 = new DataTuple(resultSchema, book1);
        List<ITuple> expectedResult = new ArrayList<>(1);
        expectedResult.add(expectedTuple1);

        Assert.assertEquals(1, resultList.size());
        Assert.assertTrue(TestUtils.equals(expectedResult, resultList));
    }


    /*
     * This case tests for the scenario when offset is some integer greater 
     * than 0 and greater than the actual number of results and join is 
     * performed.
     * Test result: An empty list.
     */
    @Test
    public void testOffsetGreaterThanNumberOfResults() throws Exception{
        List<ITuple> tuples = JoinTestConstants.bookGroup1.subList(1, 5);
        
        JoinTestHelper.insertToTable(BOOK_TABLE, tuples);
           
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "typical", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "actually", conjunction);
        
        List<ITuple> resultList = JoinTestHelper.getJoinDistanceResults(keywordSourceOuter, keywordSourceInner, 
                new JoinDistancePredicate(JoinTestConstants.REVIEW, 90), 1, 10);

        Assert.assertEquals(0, resultList.size());
    }

    // ------------------------<Test cases for cursor.>------------------------
    /*
     * This case tests for the scenario when open and/or close is called twice 
     * and also when getNextTuple() is called when operator is closed.
     * Test result: Opening or closing the operator twice shouldn't result in 
     * any noticeable difference in operation. But, calling getNetTuple() when 
     * operator is closed should throw an exception.
     */
    @Test(expected = DataFlowException.class)
    public void testWhenOpenOrCloseIsCalledTwiceAndTryToGetNextTupleWhenClosed() throws Exception {
        List<ITuple> tuples = JoinTestConstants.bookGroup1.subList(1, 5);
        
        JoinTestHelper.insertToTable(BOOK_TABLE, tuples);
           
        KeywordMatcherSourceOperator keywordSourceOuter = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "typical", conjunction);
        KeywordMatcherSourceOperator keywordSourceInner = 
                JoinTestHelper.getKeywordSource(BOOK_TABLE, "actually", conjunction);

        JoinDistancePredicate distancePredicate = new JoinDistancePredicate(JoinTestConstants.REVIEW, 90);
        
        Join join = new Join(distancePredicate);
        join.setOuterInputOperator(keywordSourceOuter);
        join.setInnerInputOperator(keywordSourceInner);
        
        ITuple tuple;
        List<ITuple> resultList = new ArrayList<>();
        
        join.open();
        join.open();
        while ((tuple = join.getNextTuple()) != null) {
            resultList.add(tuple);
        }
        join.close();
        join.close();

        Assert.assertEquals(4, resultList.size());
        
        // this line should throw an exception because operator is already closed
        if ((tuple = join.getNextTuple()) != null) {
            resultList.add(tuple);
        }
    }

}