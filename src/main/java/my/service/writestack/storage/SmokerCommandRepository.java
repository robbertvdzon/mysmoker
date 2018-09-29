package my.service.writestack.storage;

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
import my.service.common.storage.SmokerRepository;
import my.service.writestack.model.Sample;
import my.service.writestack.model.SmokerSession;
import my.service.writestack.model.SmokerState;

import java.util.*;

import static my.service.common.Const.SMOKERSAMPLES_TABLENAME;
import static my.service.common.Const.SMOKERSESSIONS_TABLENAME;
import static my.service.common.Const.SMOKERSTATE_TABLENAME;

public class SmokerCommandRepository extends SmokerRepository {

    private final AmazonDynamoDB ddb = getDynamoDb();

    public void createTablewhenNeeded() {
        createTable(ddb, SMOKERSESSIONS_TABLENAME, "id", "N");
        createTable(ddb, SMOKERSAMPLES_TABLENAME, "time", "N");
        createTable(ddb, SMOKERSTATE_TABLENAME, "id", "N");
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

        Map<String, String> attributeNames = new HashMap<String, String>();
        attributeNames.put("#id", "id");

        Map<String, AttributeValue> attributeValues = new HashMap<String, AttributeValue>();
        attributeValues.put(":id", new AttributeValue().withN("1"));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("#id = :id")
                .withExpressionAttributeNames(attributeNames)
                .withExpressionAttributeValues(attributeValues);

        List<SmokerState> allStates = mapper.scan(SmokerState.class, scanExpression);
        return allStates.stream().findFirst().orElseGet(() -> createNewState());
    }

    public SmokerSession loadSession(long sessionId) {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);

        Map<String, String> attributeNames = new HashMap<String, String>();
        attributeNames.put("#id", "id");

        Map<String, AttributeValue> attributeValues = new HashMap<String, AttributeValue>();
        attributeValues.put(":id", new AttributeValue().withN("" + sessionId));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("#id = :id")
                .withExpressionAttributeNames(attributeNames)
                .withExpressionAttributeValues(attributeValues);

        List<SmokerSession> allSessions = mapper.scan(SmokerSession.class, scanExpression);
        return allSessions.stream().findFirst().orElse(null);
    }

    public void removeSession(long sessionId) {
        removeSessionFromTable(sessionId);
        removeSamplesFromTable(sessionId);
    }

    /*
    private functions
     */

    private SmokerState createNewState() {
        SmokerState smokerState = SmokerState.builder().id(1).build();
        storeState(smokerState);
        return smokerState;
    }

    private void removeSamplesFromTable(long sessionId) {
        DynamoDB dynamoDB = new DynamoDB(ddb);
        Table table = dynamoDB.getTable(SMOKERSAMPLES_TABLENAME);
        findSamplesDates(ddb, sessionId).forEach(l -> {
            DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
                    .withPrimaryKey(new PrimaryKey("time", l));
            table.deleteItem(deleteItemSpec);
        });
    }

    private List<Long> findSamplesDates(AmazonDynamoDB ddb, long sessionId) {
        DynamoDB dynamoDB = new DynamoDB(ddb);
        Table table = dynamoDB.getTable(SMOKERSAMPLES_TABLENAME);
        ScanSpec scanSpec = new ScanSpec()
                .withFilterExpression("sessionId = :id")
                .withValueMap(new ValueMap()
                        .withNumber(":id", sessionId)
                );
        ItemCollection<ScanOutcome> items = table.scan(scanSpec);
        List<Long> result = new ArrayList<>();
        for (Item item : items) {
            long time = item.getLong("time");
            result.add(time);
        }
        return result;
    }

    private void removeSessionFromTable(long sessionId) {
        DynamoDB dynamoDB = new DynamoDB(ddb);
        Table table = dynamoDB.getTable(SMOKERSESSIONS_TABLENAME);
        DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
                .withPrimaryKey(new PrimaryKey("id", sessionId));
        table.deleteItem(deleteItemSpec);
    }

}
