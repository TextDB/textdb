package edu.uci.ics.texera.dataflow.udf;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import edu.uci.ics.texera.api.constants.ErrorMessages;
import edu.uci.ics.texera.api.dataflow.IOperator;
import edu.uci.ics.texera.api.exception.DataflowException;
import edu.uci.ics.texera.api.exception.TexeraException;
import edu.uci.ics.texera.api.field.*;
import edu.uci.ics.texera.api.schema.Attribute;
import edu.uci.ics.texera.api.schema.AttributeType;
import edu.uci.ics.texera.api.schema.Schema;
import edu.uci.ics.texera.api.span.Span;
import edu.uci.ics.texera.api.tuple.Tuple;
import edu.uci.ics.texera.api.utils.Utils;
import org.apache.arrow.flight.*;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.complex.ListVector;
import org.apache.arrow.vector.complex.StructVector;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.TimeUnit;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.util.Text;

import static edu.uci.ics.texera.api.schema.AttributeType.*;

public class PythonUDFOperator implements IOperator {
    private static final int MAX_TRY_COUNT = 20;
    private static final long WAIT_TIME_MS = 500;
    private final PythonUDFPredicate predicate;
    private IOperator inputOperator;
    private Schema outputSchema;

    private List<Tuple> tupleBuffer;
    Queue<Tuple> resultQueue;

    private int cursor = CLOSED;

    private final static String PYTHON = "python3";
    private final static String PYTHONSCRIPT = Utils.getPythonResourcePath("texera_udf_server_main.py").toString();
    private final String userScriptPath;
    private final List<String> outerFilePaths;

    // For now it is fixed, but in the future should deal with arbitrary tuple and schema.
    // Related to Apache Arrow.
    private org.apache.arrow.vector.types.pojo.Schema tupleToPythonSchema;

    private final static RootAllocator globalRootAllocator = new RootAllocator();
    private FlightClient flightClient = null;

    // This is temporary, used to vectorize LIST type data.
    private Map<String, Integer> innerIndexMap;
    private final static ObjectMapper mapper = new ObjectMapper();

    public PythonUDFOperator(PythonUDFPredicate predicate){
        this.predicate = predicate;

        userScriptPath = Utils.getPythonResourcePath(predicate.getPythonScriptName()).toString();

        List<String> outerFileNames = predicate.getOuterFileNames();
        if (!outerFileNames.isEmpty()) {
            outerFilePaths = new ArrayList<>();
            for (String s : outerFileNames) {
                outerFilePaths.add(Utils.getPythonResourcePath(s).toString());
            }
        } else outerFilePaths = null;
    }

    public void setInputOperator(IOperator operator) {
        if (cursor != CLOSED) {
            throw new TexeraException("Cannot link this operator to another operator after the operator is opened");
        }
        this.inputOperator = operator;
    }

    /*
     * add new field(s) to the schema, with name resultAttributeName and type TEXT
     */
    private Schema transformSchema(Schema inputSchema){
        for (String name :  predicate.getInputAttributeNames()) {
            Schema.checkAttributeExists(inputSchema, name);
        }
        for (String name : predicate.getResultAttributeNames()) {
            Schema.checkAttributeNotExists(inputSchema, name);
        }
        Schema transformedSchema;
        try {
            transformedSchema = convertToTexeraSchema(convertToArrowSchema(inputSchema));
        } catch (Exception e) {
            closeClientAndServer(flightClient);
            throw new TexeraException(e.getMessage(), e);
        }

        Schema.Builder resultSchemaBuilder = new Schema.Builder().add(transformedSchema);
        List<String> resultNames = predicate.getResultAttributeNames();
        List<AttributeType> resultTypes = predicate.getResultAttributeTypes();
        for (int i = 0; i < resultNames.size(); i++) {
            resultSchemaBuilder.add(resultNames.get(i), resultTypes.get(i));
        }
        return resultSchemaBuilder.build();
    }

    /**
     * When this operator is opened, it executes the python script, which constructs a {@code FlightServer}
     * object which is then up and running in the specified address. The operator calls
     * {@code flightClient.doAction(new Action("healthcheck"))} to check the status of the server, and then proceeds if
     * successful (otherwise there will be an exception).
     * @throws TexeraException
     */
    @Override
    public void open() throws TexeraException {
        if (cursor != CLOSED) {
            return;
        }
        if (inputOperator == null) {
            throw new DataflowException(ErrorMessages.INPUT_OPERATOR_NOT_SPECIFIED);
        }

        inputOperator.open();

        // Flight related
        try {
            int portNumber = getFreeLocalPort();
            Location location = new Location(URI.create("grpc+tcp://localhost:" + portNumber));
            List<String> args = new ArrayList<>(
                    Arrays.asList(PYTHON, PYTHONSCRIPT, Integer.toString(portNumber), userScriptPath)
            );

            ProcessBuilder processBuilder = new ProcessBuilder(args).inheritIO();
            // Start Flight server (Python process)
            processBuilder.start();
            // Connect to server
            boolean connected = false;
            int tryCount = 0;
            while (!connected && tryCount < MAX_TRY_COUNT) {
                try {
                    Thread.sleep(WAIT_TIME_MS);
                    flightClient = FlightClient.builder(globalRootAllocator, location).build();
                    String message = new String(
                            flightClient.doAction(new Action("healthcheck")).next().getBody(), StandardCharsets.UTF_8);
                    connected = message.equals("Flight Server is up and running!");
                } catch (Exception e) {
                    System.out.println("Flight Client:\tNot connected to the server in this try.");
                    flightClient.close();
                    tryCount++;
                }
            }
            if (tryCount == MAX_TRY_COUNT) throw new DataflowException("Exceeded try limit of 5 when connecting to Flight Server!");
        } catch (Exception e) {
            throw new DataflowException(e.getMessage(), e);
        }

        // Send user args to Server.
        List<String> userArgs = new ArrayList<>();
        if (predicate.getInputAttributeNames() != null) userArgs.addAll(predicate.getInputAttributeNames());
        if (predicate.getResultAttributeNames() != null) userArgs.addAll(predicate.getResultAttributeNames());
        if (outerFilePaths != null) userArgs.addAll(outerFilePaths);

        Schema argsSchema = new Schema(new Attribute("args", TEXT));
        List<Tuple> argsTuples = new ArrayList<>();
        for (String arg : userArgs) {
            argsTuples.add(new Tuple(argsSchema, new TextField(arg)));
        }
        writeArrowStream(argsTuples, globalRootAllocator, convertToArrowSchema(argsSchema), "args",
                predicate.getChunkSize());
        // Let server open Python UDF
        try{
            flightClient.doAction(new Action("open")).next().getBody();
        }catch(Exception e){
            closeClientAndServer(flightClient);
            throw new DataflowException(e.getMessage(), e);
        }

        Schema inputSchema = inputOperator.getOutputSchema();

        // generate output schema by transforming the input schema
        outputSchema = transformToOutputSchema(inputSchema);

        cursor = OPENED;

        tupleToPythonSchema = convertToArrowSchema(inputSchema);
        innerIndexMap = new HashMap<>();
    }

    /**
     * For every batch, the operator calls {@code flightClient.doAction(new Action("compute"))} to tell the server to
     * compute sentiments of the specific table that was sent earlier. The server executes computation,
     * and returns back a success message when computation is finished.
     * @return Whether the buffer is empty
     */
    private boolean computeTupleBuffer() {
        tupleBuffer = new ArrayList<Tuple>();
        int i = 0;
        while (i < predicate.getBatchSize()){
            Tuple inputTuple;
            if ((inputTuple = inputOperator.getNextTuple()) != null) {
                tupleBuffer.add(inputTuple);
                i++;
            } else {
                break;
            }
        }
        if (tupleBuffer.isEmpty()) {
            return false;
        }
        writeArrowStream(tupleBuffer, globalRootAllocator, tupleToPythonSchema, "toPython", predicate.getChunkSize());
        return true;
    }

    @Override
    public Tuple getNextTuple() throws TexeraException {
        if (cursor == CLOSED) {
            return null;
        }

        // Not necessarily one input to one output mapping now.

        if (resultQueue != null) {
            // UDF execution has been made, retrieve results.
            if (resultQueue.isEmpty()) {
                // Try to make one more batch execution
                if (computeTupleBuffer()) {
                    executeUDF();
                }
                // All the tuples have been sent to Python and no result remain.
                if (resultQueue.isEmpty()) return null;
                else return resultQueue.remove();
            } else return resultQueue.remove();
        } else {
            // No execution of UDF has been made yet.
            if (tupleBuffer == null){
                if (computeTupleBuffer()) {
                    executeUDF();
                } else {
                    // No data at all
                    tupleBuffer = null;
                    return null;
                }
            }
            if (resultQueue == null || resultQueue.isEmpty()) return null;
            else return resultQueue.remove();
        }

    }

    // Process the data file using NLTK
    private void executeUDF() {
        try{
            flightClient.doAction(new Action("compute")).next().getBody();
            resultQueue = new LinkedList<>();
            readArrowStream();
        }catch(Exception e){
            closeClientAndServer(flightClient);
            e.printStackTrace();
            throw new DataflowException(e);
        }
    }

    /**
     * When all the batches are finished and the operator closes, it issues a
     * {@code flightClient.doAction(new Action("shutdown"))} call to shut down the server, and also closes the client.
     * @throws TexeraException
     */
    @Override
    public void close() throws TexeraException {
        closeClientAndServer(flightClient);
        if (cursor == CLOSED) {
            return;
        }
        if (inputOperator != null) {
            inputOperator.close();
        }
        cursor = CLOSED;
    }

    @Override
    public Schema getOutputSchema() {
        return this.outputSchema;
    }

    public Schema transformToOutputSchema(Schema... inputSchema) {

        if (inputSchema.length != 1)
            throw new TexeraException(String.format(ErrorMessages.NUMBER_OF_ARGUMENTS_DOES_NOT_MATCH, 1, inputSchema.length));

        // check if the input schema is presented
        for (String name : predicate.getInputAttributeNames()) {
            if (! inputSchema[0].containsAttribute(name)) {
                throw new TexeraException(String.format(
                        "input attribute %s is not in the input schema %s",
                        name,
                        inputSchema[0].getAttributeNames()));
            }
        }

        return transformSchema(inputSchema[0]);
    }

    private static void vectorizeTupleToPython(Tuple tuple, int index, VectorSchemaRoot schemaRoot,
                                               Map<String, Integer> innerIndexMap) throws Exception {
        for (Attribute a : tuple.getSchema().getAttributes()) {
            String name = a.getName();
            // When it is null, skip it.
            if (tuple.getField(name).getValue() == null) continue;
            switch (a.getType()) {
                case INTEGER:
                    ((IntVector) schemaRoot.getVector(name)).setSafe(index, (int) tuple.getField(name).getValue());
                    break;
                case DOUBLE:
                    ((Float8Vector) schemaRoot.getVector(name)).setSafe(index, (double) tuple.getField(name).getValue());
                    break;
                case BOOLEAN:
                    // Current BOOLEAN type is internally a string, so it will fall-through to the string case.
                    // We might change this in the future.
                case TEXT:
                case STRING:
                case _ID_TYPE:
                    ((VarCharVector) schemaRoot.getVector(name)).setSafe(
                            index, tuple.getField(name).getValue().toString().getBytes(StandardCharsets.UTF_8));
                    break;
                case DATE:
                    ((DateDayVector) schemaRoot.getVector(name)).setSafe(index,
                            (int)((LocalDate) tuple.getField(name).getValue()).toEpochDay());
                    break;
                case DATETIME:
                    StructVector dateTimeStructs = ((StructVector) schemaRoot.getVector(name));
                    if (tuple.getField(name).getValue() != null) {
                        dateTimeStructs.setIndexDefined(index);
                        DateDayVector subVectorDay = (DateDayVector) dateTimeStructs.getVectorById(0);
                        TimeSecVector subVectorTime = (TimeSecVector) dateTimeStructs.getVectorById(1);
                        LocalDateTime value = (LocalDateTime) tuple.getField(name).getValue();
                        subVectorDay.setSafe(index, (int) value.toLocalDate().toEpochDay());
                        subVectorTime.setSafe(index, value.toLocalTime().toSecondOfDay());
                    }
                    else dateTimeStructs.setNull(index);
                    break;
                case LIST:
                    // For now only supporting span.
                    if (((ImmutableList) tuple.getField(name).getValue()).get(0).getClass() != Span.class) {
                        schemaRoot.clear();
                        throw (new Exception("Unsupported Element Type for List Field!"));
                    }
                    else {
                        ListVector listVector = (ListVector) schemaRoot.getVector(name);
                        ImmutableList<Span> spansList = (ImmutableList<Span>) tuple.getField(name).getValue();
                        try {
                            convertListOfSpans(spansList, listVector, index, name, innerIndexMap);
                        } catch (JsonProcessingException e) {
                            schemaRoot.clear();
                            throw new Exception(e.getMessage(), e);
                        }
                    }

                    break;
                default: break;
            }
        }
    }

    /**
     * For every batch, the operator converts list of {@code Tuple}s into Arrow stream data in almost the exact same
     * way as it would when using Arrow file, except now it sends stream to the server with
     * {@link FlightClient#startPut(org.apache.arrow.flight.FlightDescriptor, org.apache.arrow.vector.VectorSchemaRoot,
     * org.apache.arrow.flight.FlightClient.PutListener, org.apache.arrow.flight.CallOption...)} and {@link
     * FlightClient.ClientStreamListener#putNext()}. The server uses {@code do_put()} to receive data stream
     * and convert it into a {@code pyarrow.Table} and store it in the server.
     * @param values The buffer of tuples to write.
     */
    private void writeArrowStream(
            List<Tuple> values,
            RootAllocator root,
            org.apache.arrow.vector.types.pojo.Schema arrowSchema,
            String descriptorPath,
            int chunkSize) {
        SyncPutListener flightListener = new SyncPutListener();
        VectorSchemaRoot schemaRoot = VectorSchemaRoot.create(arrowSchema, root);
        FlightClient.ClientStreamListener streamWriter = flightClient.startPut(
                FlightDescriptor.path(Collections.singletonList(descriptorPath)), schemaRoot, flightListener);
        try {
            int index = 0;
            while (index < values.size()) {
                schemaRoot.allocateNew();
                int chunkIndex = 0;
                while (chunkIndex < chunkSize && index + chunkIndex < values.size()) {
                    vectorizeTupleToPython(values.get(index + chunkIndex), chunkIndex, schemaRoot, innerIndexMap);
                    chunkIndex++;
                }
                schemaRoot.setRowCount(chunkIndex);
                streamWriter.putNext();
                index += chunkIndex;
                schemaRoot.clear();
            }
            streamWriter.completed();
            flightListener.getResult();
            flightListener.close();
            schemaRoot.clear();
        } catch (Exception e) {
            closeClientAndServer(flightClient);
            throw new TexeraException(e.getMessage(), e);
        }
    }


    /**
     * For every batch, the operator gets the computed sentiment result by calling
     * {@link FlightClient#getStream(org.apache.arrow.flight.Ticket, org.apache.arrow.flight.CallOption...)}.
     * The reading and conversion process is the same as what it does when using Arrow file.
     */
    private void readArrowStream() {
        try {
            FlightInfo info = flightClient.getInfo(FlightDescriptor.path(Collections.singletonList("FromPython")));
            Ticket ticket = info.getEndpoints().get(0).getTicket();
            FlightStream stream = flightClient.getStream(ticket);
            while (stream.next()) {
                VectorSchemaRoot root  = stream.getRoot(); // get root
                convertArrowVectorsToResults(root, resultQueue);
                root.clear();
            }
        } catch (Exception e) {
            closeClientAndServer(flightClient);
            throw new TexeraException(e.getMessage(), e);
        }
    }

    private static int getFreeLocalPort() throws IOException {
        ServerSocket s = null;
        try {
            // ServerSocket(0) results in availability of a free random port
            s = new ServerSocket(0);
            return s.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            assert s != null;
            s.close();
        }
    }

    private static org.apache.arrow.vector.types.pojo.Schema convertToArrowSchema(Schema texeraSchema) {
        List<Field> arrowFields = new ArrayList<>();
        for (Attribute a : texeraSchema.getAttributes()) {
            String name = a.getName();
            Field field = null;
            switch (a.getType()) {
                case INTEGER:
                    field = Field.nullablePrimitive(name, new ArrowType.Int(32, true));
                    break;
                case DOUBLE:
                    field = Field.nullablePrimitive(name, new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE));
                    break;
                case BOOLEAN:
                    // Current BOOLEAN type is internally a string, so it will fall-through to the string case.
                    // We might change this in the future.
                case TEXT:
                case STRING:
                case _ID_TYPE:
                    field = Field.nullablePrimitive(name, ArrowType.Utf8.INSTANCE);
                    break;
                case DATE:
                    field = Field.nullablePrimitive(name, new ArrowType.Date(DateUnit.DAY));
                    break;
                case DATETIME:
                    field = new Field(
                            name,
                            FieldType.nullable(ArrowType.Struct.INSTANCE),
                            Arrays.asList(
                                    Field.nullablePrimitive("date", new ArrowType.Date(DateUnit.DAY)),
                                    Field.nullablePrimitive("time", new ArrowType.Time(TimeUnit.SECOND, 32))
                            )
                    );
                    break;
                case LIST:
                    // Because of the bug of Apache Arrow that it cannot convert List of Structs from pandas to Arrow,
                    // We're now temporarily converting Span to Json String.
//                    List<Field> children = Arrays.asList(
//                            Field.nullablePrimitive("attributeName", ArrowType.Utf8.INSTANCE),
//                            Field.nullablePrimitive("start", new ArrowType.Int(32, true)),
//                            Field.nullablePrimitive("end", new ArrowType.Int(32, true)),
//                            Field.nullablePrimitive("key", ArrowType.Utf8.INSTANCE),
//                            Field.nullablePrimitive("value", ArrowType.Utf8.INSTANCE),
//                            Field.nullablePrimitive("tokenOffset", new ArrowType.Int(32, true))
//                    );
                    field = new Field(
                            name,
                            FieldType.nullable(new ArrowType.List()),
                            Collections.singletonList(Field.nullablePrimitive("Span", ArrowType.Utf8.INSTANCE))
                            );
//                                    new Field("Span", FieldType.nullable(ArrowType.Struct.INSTANCE), children))
                    break;
                default: break;
            }
            arrowFields.add(field);
        }
        return new org.apache.arrow.vector.types.pojo.Schema(arrowFields);
    }

    private static void convertArrowVectorsToResults(VectorSchemaRoot schemaRoot, Queue<Tuple> resultQueue)
            throws Exception {
        List<FieldVector> fieldVectors = schemaRoot.getFieldVectors();
        Schema texeraSchema = convertToTexeraSchema(schemaRoot.getSchema());
        for (int i = 0; i < schemaRoot.getRowCount(); i++) {
            Tuple tuple;
            List<IField> texeraFields = new ArrayList<>();

            for (FieldVector vector : fieldVectors) {
                IField texeraField = null;
                try {
                    switch (vector.getField().getFieldType().getType().getTypeID()) {
                        case Int:
                            // It's either IntVector or BigIntVector, but can't know because it depends on Python.
                            try {
                                texeraField = new IntegerField(((IntVector) vector).get(i));
                            } catch (ClassCastException e) {
                                texeraField = new IntegerField((int)((BigIntVector) vector).get(i));
                            }
                            break;
                        case FloatingPoint:
                            texeraField = new DoubleField((((Float8Vector) vector).get(i)));
                            break;
//                    case Bool: // FIXME: No BooleanField Class available.
                        case Utf8:
                            texeraField = new TextField(new String(((VarCharVector) vector).get(i), StandardCharsets.UTF_8));
                            break;
                        case Date:
                            texeraField = new DateField(new Date(((DateDayVector) vector).get(i)));
                            break;
                        case Struct:
                            // For now, struct is only for DateTime
                            DateDayVector subVectorDay = (DateDayVector) ((StructVector) vector).getChildByOrdinal(0);
                            try{
                                TimeSecVector subVectorTime = (TimeSecVector) ((StructVector) vector).getChildByOrdinal(1);
                                texeraField = new DateTimeField(
                                        LocalDateTime.of(
                                                LocalDate.ofEpochDay(subVectorDay.get(i)),
                                                LocalTime.ofSecondOfDay(subVectorTime.get(i))
                                        )
                                );
                            } catch (ClassCastException e) {
                                TimeMicroVector subVectorTime = (TimeMicroVector) ((StructVector) vector).getChildByOrdinal(1);
                                texeraField = new DateTimeField(
                                        LocalDateTime.of(
                                                LocalDate.ofEpochDay(subVectorDay.get(i)),
                                                LocalTime.ofSecondOfDay(subVectorTime.get(i)/1000000)
                                        )
                                );
                            }
                            break;
                        case List:
                            texeraField = getSpanFromListVector((ListVector) vector, i);
                            break;
                        default:
                            schemaRoot.clear();
                            throw (new Exception("Unsupported data type "+ vector.getField().toString() +
                                    " when converting back to Texera table."));
                    }
                } catch (IllegalStateException | IOException e) {
                    if (!e.getMessage().contains("Value at index is null")) {
                        throw new Exception(e.getMessage(), e);
                    } else {
                        switch (vector.getField().getFieldType().getType().getTypeID()) {
                            case Int: texeraField = new IntegerField(null); break;
                            case FloatingPoint: texeraField = new DoubleField(null); break;
                            case Date: texeraField = new DateField((String) null); break;
                            case Struct: texeraField = new DateTimeField((String) null); break;
                            case List: texeraField = new ListField<Span>(null);
                            default: break;
                        }
                    }
                }
                texeraFields.add(texeraField);
            }
            tuple = new Tuple(texeraSchema, texeraFields);
            resultQueue.add(tuple);
        }
    }

    private static Schema convertToTexeraSchema(org.apache.arrow.vector.types.pojo.Schema arrowSchema)
            throws Exception {
        List<Attribute> texeraAttributes = new ArrayList<>();
        for (Field f : arrowSchema.getFields()) {
            String attributeName = f.getName();
            AttributeType attributeType;
            ArrowType arrowType = f.getFieldType().getType();
            switch (arrowType.getTypeID()) {
                case Int:
                    attributeType = INTEGER;
                    break;
                case FloatingPoint:
                    attributeType = DOUBLE;
                    break;
                case Bool:
                    attributeType = BOOLEAN;
                    break;
                case Utf8:
                case Null:
                    attributeType = TEXT;
                    break;
                case Date:
                    attributeType = DATE;
                    break;
                case Struct:
                    // For now only Struct of DateTime
                    attributeType = DATETIME;
                    break;
                case List:
                    attributeType = LIST;
                    break;
                default:
                    throw (new Exception("Unsupported data type "+
                            arrowType.getTypeID() +
                            " when converting back to Texera table."));
            }
            texeraAttributes.add(new Attribute(attributeName, attributeType));
        }
        return new Schema(texeraAttributes);
    }

    // For now we're only allowing List<Span>. This can (and should) be generalized in the future.
    private static void convertListOfSpans(ImmutableList<Span> spansList, ListVector listVector, int index, String name,
                                           Map<String, Integer> innerIndexMap) throws JsonProcessingException {
        if (index == 0) {
            if (innerIndexMap.containsKey(name)) innerIndexMap.replace(name, 0);
            else innerIndexMap.put(name, 0);
        }
        int innerIndex = innerIndexMap.get(name);
        int size = spansList.size();
        VarCharVector subElementsVector = (VarCharVector) listVector.getDataVector();
//        StructVector subElementsVector = (StructVector) listVector.getDataVector();
        listVector.startNewValue(index);
//        VarCharVector attributeNameVector = (VarCharVector) subElementsVector.getVectorById(0);
//        IntVector startVector = (IntVector) subElementsVector.getVectorById(1);
//        IntVector endVector = (IntVector) subElementsVector.getVectorById(2);
//        VarCharVector keyVector = (VarCharVector) subElementsVector.getVectorById(3);
//        VarCharVector valueVector = (VarCharVector) subElementsVector.getVectorById(4);
//        IntVector tokenOffsetVector = (IntVector) subElementsVector.getVectorById(5);
        for (int i = 0; i < size; i++) {
            if (spansList.get(i) == null) {
                subElementsVector.setNull(innerIndex);
            }
            else {
                subElementsVector.setIndexDefined(innerIndex);
                Span span = spansList.get(i);
//                // For all the fields of the struct
//                if (span.getAttributeName() != null) attributeNameVector.setSafe(innerIndex, span.getAttributeName().getBytes(StandardCharsets.UTF_8));
//                startVector.setSafe(innerIndex, span.getStart());
//                endVector.setSafe(innerIndex, span.getEnd());
//                if (span.getKey() != null) keyVector.setSafe(innerIndex, span.getKey().getBytes(StandardCharsets.UTF_8));
//                if (span.getValue() != null) valueVector.setSafe(innerIndex, span.getValue().getBytes(StandardCharsets.UTF_8));
//                tokenOffsetVector.setSafe(innerIndex, span.getTokenOffset());
                subElementsVector.setSafe(innerIndex, mapper.writeValueAsBytes(span));

            }
            innerIndex++;
        }
        innerIndexMap.replace(name, innerIndex);
        listVector.endValue(index, size);
    }

    private static ListField<Span> getSpanFromListVector(ListVector listVector, int index) throws IOException {
       List<Span> resultList = new ArrayList<>();
       List<Text> vals = (List<Text>) listVector.getObject(index);
       for (Text spanJsonText : vals) {
           resultList.add(
//                   new Span(
//                           spanMap.get("attributeName").toString(),
//                           (int) spanMap.get("start"),
//                           (int) spanMap.get("end"),
//                           spanMap.get("key").toString(),
//                           spanMap.get("value").toString(),
//                           (int) spanMap.get("tokenOffset")
//                   )
                   mapper.readValue(spanJsonText.getBytes(), Span.class)
           );

       }
       return new ListField<>(resultList);
    }

    private static void closeClientAndServer(FlightClient flightClient) {
        try {
            flightClient.doAction(new Action("close")).next().getBody();
            flightClient.doAction(new Action("shutdown")).next();
            globalRootAllocator.close();
            flightClient.close();
        } catch (Exception e) {
            throw new DataflowException(e.getMessage(), e);
        }
    }
}

