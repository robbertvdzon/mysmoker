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
import my.service.readstack.model.JsonSmokerState;
import my.service.writestack.model.SmokerSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class SmokerQueryRepository {
    private AmazonDynamoDB ddb;
    private Table smokersamplesTable;
    private Table smokersessionsTable;
    private Table smokerstateTable;

    public SmokerQueryRepository() {
        ddb = getDynamoDb();
        DynamoDB dynamoDB = new DynamoDB(ddb);
        smokersamplesTable = dynamoDB.getTable("smokersamples");
        smokersessionsTable = dynamoDB.getTable("smokersessions");
        smokerstateTable = dynamoDB.getTable("smokerstate");
    }

    public List<JsonSample> findSamples(long sessionStartTime, boolean lowSamples) {
        ScanSpec scanSpec = getSamplesScanSpec(sessionStartTime, lowSamples);
        return getScanResult(scanSpec, smokersamplesTable, this::toJsonSample, JsonSample::compareTo);
    }

    public SmokerSession findLastSession() {
        long sessionDatetime = loadState().getCurrentSessionStartTime();
        return findSession(session -> session.getSessionStartTime() == sessionDatetime);
    }

    public SmokerSession findSession(String dateTimeString) {
        return findSession(session -> session.getSessionDateTime().equals(dateTimeString));
    }

    public List<String> listAllSessionIds() {
        ScanSpec scanSpec = new ScanSpec();
        return getScanResult(scanSpec, smokersessionsTable, this::getSessionDateTime, String::compareTo);
    }

    public JsonSmokerState loadState() {
        ScanSpec scanSpec = new ScanSpec();
        return getScanResult(scanSpec, smokerstateTable, this::toSmokerState, JsonSmokerState::compareTo)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("State not found"));
    }

    //---------------------
    private AmazonDynamoDB getDynamoDb() {
        return AmazonDynamoDBClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();
    }

    private JsonSample toJsonSample(Item item) {
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
        return sample;
    }

    private JsonSmokerState toSmokerState(Item item) {
        int id = item.getInt("id");
        long currentSessionStartTime = item.getLong("currentSessionStartTime");
        double bbqTempSet = item.getDouble("bbqTempSet");
        JsonSmokerState smokerState = new JsonSmokerState();
        smokerState.setId(id);
        smokerState.setCurrentSessionStartTime(currentSessionStartTime);
        smokerState.setBbqTempSet(bbqTempSet);
        return smokerState;
    }

    private String getSessionDateTime(Item item) {
        return item.getString("sessionDateTime");
    }

    public SmokerSession findSession(Predicate<? super SmokerSession> predicate) {
        return listAllSessions(ddb)
                .stream()
                .filter(predicate)
                .findFirst()
                .orElse(null);
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

    private <T> List<T> getScanResult(ScanSpec scanSpec, Table table, Function<Item, T> toObject, Comparator<? super T> c) {
        ItemCollection<ScanOutcome> items = table.scan(scanSpec);
        List<T> result = new ArrayList<>();
        for (Item item : items) {
            result.add(toObject.apply(item));
        }
        result.sort(c);
        return result;
    }

    private List<SmokerSession> listAllSessions(AmazonDynamoDB ddb) {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        return mapper.scan(SmokerSession.class, scanExpression);
    }




}
