package my.service.readstack;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import my.service.readstack.model.JsonSample;
import my.service.readstack.model.JsonSmokerSession;
import my.service.readstack.model.JsonSmokerState;

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

    public JsonSmokerSession findLastSession(Function<Long, Boolean> useLowSamples) {
        long sessionDatetime = loadState().getCurrentSessionStartTime();
        JsonSmokerSession result = findSession(session -> session.getSessionStartTime() == sessionDatetime, useLowSamples);
        return result;
    }

    public JsonSmokerSession findSession(String dateTimeString, Function<Long, Boolean> useLowSamples) {
        return findSession(session -> session.getSessionDateTime().equals(dateTimeString), useLowSamples);
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

    private List<JsonSmokerSession> listAllSessions(Function<Long, Boolean> useLowSamples) {
        ScanSpec scanSpec = new ScanSpec();
        return getScanResult(scanSpec, smokersessionsTable, s -> toJsonSmokerSessionWithoutSamples(s), JsonSmokerSession::compareTo);
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

    private JsonSmokerSession toJsonSmokerSessionWithoutSamples(Item item) {
        String sessionDateTime = item.getString("sessionDateTime");
        long lastSampleTime = item.getLong("lastSampleTime");
        long sessionStartTime = item.getLong("sessionStartTime");
        long samplesCount = item.getLong("samplesCount");
        double lastBbqTemp = item.getDouble("lastBbqTemp");
        double lastMeatTemp = item.getDouble("lastMeatTemp");
        double lastFan = item.getDouble("lastFan");
        double lastBbqSet = item.getDouble("lastBbqSet");

        return JsonSmokerSession.builder()
                .sessionDateTime(sessionDateTime)
                .sessionStartTime(sessionStartTime)
                .samplesCount(samplesCount)
                .lastSampleTime(lastSampleTime)
                .lastBbqTemp(lastBbqTemp)
                .lastBbqSet(lastBbqSet)
                .lastMeatTemp(lastMeatTemp)
                .lastFan(lastFan)
                .build();
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

    public JsonSmokerSession findSession(Predicate<? super JsonSmokerSession> predicate, Function<Long, Boolean> useLowSamples) {
        return listAllSessions(useLowSamples)
                .stream()
                .filter(predicate)
                .findFirst()
                .map(s -> this.addSamples(s, useLowSamples))
                .orElse(null);
    }

    private JsonSmokerSession addSamples(JsonSmokerSession session, Function<Long, Boolean> useLowSamples) {
        long samplesCount = session.getSamplesCount();
        long sessionStartTime = session.getSessionStartTime();
        Boolean lowSamples = useLowSamples.apply(samplesCount);
        List<JsonSample> samples = findSamples(sessionStartTime, lowSamples);
        return session.toBuilder().samples(samples).build();
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
}
