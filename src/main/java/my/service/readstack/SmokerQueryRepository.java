package my.service.readstack;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import my.service.readstack.model.JsonSample;
import my.service.writestack.model.SmokerSession;
import my.service.writestack.model.SmokerState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SmokerQueryRepository {
    private AmazonDynamoDB ddb = getDynamoDb();


    public List<JsonSample> findSamples(long sessionStartTime, boolean lowSamples) {
        DynamoDB dynamoDB = new DynamoDB(ddb);
        Table table = dynamoDB.getTable("smokersamples");
        ScanSpec scanSpec = getSamplesScanSpec(sessionStartTime, lowSamples);

        ItemCollection<ScanOutcome> items = table.scan(scanSpec);
        List<JsonSample> result = new ArrayList<>();
        for (Item item : items) {
            double bbqTemp = item.getDouble("bbqTemp");
            double meatTemp = item.getDouble("meatTemp");
            double fan = item.getDouble("fan");
            double bbqSet = item.getDouble("bbqSet");
            long time = item.getLong("time");
            JsonSample sample = new JsonSample();
            sample.setBt(bbqTemp);
            sample.setBs(bbqSet);
            sample.setMt(meatTemp);
            sample.setF(fan);
            sample.setT(time);
            result.add(sample);
        }
        // the list needs to be sorted!
        // this is because the scan does not have any ordering. TODO: use a query: these do have an ordering
        result.sort((m1, m2) -> m1.getT() < m2.getT() ? -1 : 1);

        return result;
    }

    public SmokerSession findLastSession() {
        SmokerState smokerState = loadState();
        System.out.println("smokerState=" + smokerState);
        long sessionDatetime = smokerState.getCurrentSessionStartTime();

        List<SmokerSession> scanResult = listAllSessions(ddb);
        Optional<SmokerSession> last = scanResult.stream().filter(session -> session.getSessionStartTime() == sessionDatetime).findFirst();
        return last.orElse(null);
    }

    public List<SmokerSession> findSession(String dateTimeString) {


/*
    FIX THIS CODE
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        scanExpression.addExpressionAttributeNamesEntry("sessionDateTime", dateTimeString);
        return mapper.scan(SmokerSession.class, scanExpression);
*/
        return listAllSessions(ddb)
                .stream()
                .filter(s -> s.getSessionDateTime().equals(dateTimeString))
                .collect(Collectors.toList());

    }

    public SmokerState loadState() {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        List<SmokerState> allStates = mapper.scan(SmokerState.class, scanExpression);
        Optional<SmokerState> stateOptional = allStates.stream().filter(s -> s.getId() == 1).findFirst();
        return stateOptional.orElseThrow(() -> new RuntimeException("State not found"));
    }

    public List<String> listAllSessionIds() {
        DynamoDB dynamoDB = new DynamoDB(ddb);
        Table table = dynamoDB.getTable("smokersessions");
        ScanSpec scanSpec;
        scanSpec = new ScanSpec();
        ItemCollection<ScanOutcome> items = table.scan(scanSpec);
        List<String> result = new ArrayList<>();
        for (Item item : items) {
            result.add(item.getString("sessionDateTime"));
        }
        result.sort(String::compareTo);
        return result;
    }

    //---------------------

    private AmazonDynamoDB getDynamoDb() {
        return AmazonDynamoDBClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();
    }


    private List<SmokerSession> listAllSessions(AmazonDynamoDB ddb) {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        return mapper.scan(SmokerSession.class, scanExpression);
    }

    private ScanSpec getSamplesScanSpec(long sessionStartTime, boolean lowSamples) {
        ScanSpec scanSpec;
        if (lowSamples) {
            scanSpec = new ScanSpec()
                    .withFilterExpression("sessionStartTime = :sessionStartTime AND newMinute = :newMinute")
                    .withValueMap(new ValueMap()
                            .withNumber(":sessionStartTime", sessionStartTime)
                            .withNumber(":newMinute", 1)
                    );
        } else {
            scanSpec = new ScanSpec()
                    .withFilterExpression("sessionStartTime = :sessionStartTime")
                    .withValueMap(new ValueMap()
                            .withNumber(":sessionStartTime", sessionStartTime)
                    );
        }
        return scanSpec;
    }


}
