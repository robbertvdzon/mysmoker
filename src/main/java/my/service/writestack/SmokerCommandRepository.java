package my.service.writestack;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.*;
import my.service.writestack.model.Sample;
import my.service.writestack.model.SmokerSession;
import my.service.writestack.model.SmokerState;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class SmokerCommandRepository {

    private final AmazonDynamoDB ddb = getDynamoDb();

    public void createTablewhenNeeded() {
        createTable(ddb, "smokersessions", "sessionDateTime", "S");
        createTable(ddb, "smokersamples", "time", "N");
        createTable(ddb, "smokerstate", "id", "N");
    }


    public void storeSession(SmokerSession smokerSession) {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        mapper.save(smokerSession);
        System.out.println("session stored:" + smokerSession);

    }

    public void storeSample(Sample sample) {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        mapper.save(sample);
    }

    public void storeState(SmokerState smokerState) {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        mapper.save(smokerState);
    }

    public SmokerState loadState() {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        List<SmokerState> allStates = mapper.scan(SmokerState.class, scanExpression);
        Optional<SmokerState> stateOptional = allStates.stream().filter(s -> s.getId() == 1).findFirst();

        return stateOptional.orElseGet(() -> createNewState());
    }

    public SmokerSession findOrCreateSession(long currentTimestamp) {
        long timeout = currentTimestamp - 1000 * 60 * 60;// minus one hour
        SmokerState smokerState = loadState();
        SmokerSession lastSession = findSession(smokerState.getCurrentSessionStartTime());
        if (lastSession != null && lastSession.getLastSampleTime() > timeout) {
            return lastSession;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateString = simpleDateFormat.format(new Date(currentTimestamp));
        smokerState.setCurrentSessionStartTime(currentTimestamp);
        storeState(smokerState);
        return new SmokerSession(currentDateString, currentTimestamp);
    }

    public List<SmokerSession> findSession(String dateTimeString) {
/*
    FIX THIS CODE
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        scanExpression.addExpressionAttributeNamesEntry("sessionDateTime", dateTimeString);
        return mapper.scan(SmokerSession.class, scanExpression);
*/
        return listAllSessions()
                .stream()
                .filter(s -> s.getSessionDateTime().equals(dateTimeString))
                .collect(Collectors.toList());

    }

    private SmokerState createNewState() {
        SmokerState smokerState = SmokerState.builder().id(1).build();
        storeState(smokerState);
        return smokerState;
    }

    private AmazonDynamoDB getDynamoDb() {
        return AmazonDynamoDBClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();
    }


    private SmokerSession findSession(long sessionStartTime) {
        List<SmokerSession> allSessions = listAllSessions();
        Optional<SmokerSession> session = allSessions.stream().filter(s -> s.getSessionStartTime() == sessionStartTime).findFirst();
        return session.orElse(null);
    }

    private List<SmokerSession> listAllSessions() {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        return mapper.scan(SmokerSession.class, scanExpression);
    }


    private void createTable(AmazonDynamoDB dynamoDB, String tablename, String keyName, String keyType) {
        ListTablesResult listTablesResult = dynamoDB.listTables();
        boolean tableFound = listTablesResult.getTableNames().stream().anyMatch(name -> name.equals(tablename));
        if (!tableFound) {
            ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
            keySchema.add(new KeySchemaElement().withAttributeName(keyName).withKeyType(KeyType.HASH)); // Partition

            ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
            attributeDefinitions
                    .add(new AttributeDefinition().withAttributeName(keyName).withAttributeType(keyType));

            CreateTableRequest createTableRequest = new CreateTableRequest()
                    .withTableName(tablename)
                    .withKeySchema(keySchema)
                    .withAttributeDefinitions(attributeDefinitions)
                    .withProvisionedThroughput(new ProvisionedThroughput()
                            .withReadCapacityUnits(5l)
                            .withWriteCapacityUnits(5l)
                    );
            dynamoDB.createTable(createTableRequest);
        }
    }

    public void removeSamplesFromTable(long sessionStartTime) {
        DynamoDB dynamoDB = new DynamoDB(ddb);
        Table table = dynamoDB.getTable("smokersamples");
        System.out.println("delete all samples from session " + sessionStartTime);
        findSamplesDates(ddb, sessionStartTime).forEach(l -> {
            System.out.println("delete all samples with time " + l);
            DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
                    .withPrimaryKey(new PrimaryKey("time", l));
            table.deleteItem(deleteItemSpec);

        });
    }

    private List<Long> findSamplesDates(AmazonDynamoDB ddb, long sessionStartTime) {
        DynamoDB dynamoDB = new DynamoDB(ddb);
        Table table = dynamoDB.getTable("smokersamples");
        ScanSpec scanSpec = new ScanSpec()
                .withFilterExpression("sessionStartTime = :sessionStartTime")
                .withValueMap(new ValueMap()
                        .withNumber(":sessionStartTime", sessionStartTime)
                );
        ItemCollection<ScanOutcome> items = table.scan(scanSpec);
        List<Long> result = new ArrayList<>();
        Iterator<Item> iter = items.iterator();
        while (iter.hasNext()) {
            Item item = iter.next();
            long time = item.getLong("time");
            result.add(time);
        }
        return result;

    }


    public void removeSessionFromTable(String sessionId) {
        DynamoDB dynamoDB = new DynamoDB(ddb);
        Table table = dynamoDB.getTable("smokersessions");
        DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
                .withPrimaryKey(new PrimaryKey("sessionDateTime", sessionId));
        table.deleteItem(deleteItemSpec);
    }


}
