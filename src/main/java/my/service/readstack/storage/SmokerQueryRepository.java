package my.service.readstack.storage;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import my.service.common.storage.SmokerRepository;
import my.service.readstack.model.JsonSample;
import my.service.readstack.model.JsonSmokerSession;
import my.service.readstack.model.JsonSmokerState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static my.service.common.Const.SMOKERSAMPLES_TABLENAME;
import static my.service.common.Const.SMOKERSESSIONS_TABLENAME;
import static my.service.common.Const.SMOKERSTATE_TABLENAME;

public class SmokerQueryRepository extends SmokerRepository {
    private Table smokersamplesTable;
    private Table smokersessionsTable;
    private Table smokerstateTable;

    public SmokerQueryRepository() {
        initializeTables();
    }

    public JsonSmokerSession findLastSession(Function<Long, Boolean> useLowSamples) {
        long sessionId = loadState().getCurrentSessionId();
        JsonSmokerSession result = findSession(session -> session.getId() == sessionId, useLowSamples);
        return result;
    }

    public JsonSmokerSession findSession(Long sessionId, Function<Long, Boolean> useLowSamples) {
        return findSession(session -> session.getId()==sessionId, useLowSamples);
    }

    public List<JsonSmokerSession> listAllSessions() {
        ScanSpec scanSpec = new ScanSpec();
        return getScanResult(scanSpec, smokersessionsTable, JsonSmokerSession::fromItemWithoutSamples,  JsonSmokerSession::compareTo);
    }

    public JsonSmokerState loadState() {
        ScanSpec scanSpec = new ScanSpec();
        return getScanResult(scanSpec, smokerstateTable, JsonSmokerState::fromItem, JsonSmokerState::compareTo)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("State not found"));
    }

    /*
     Private functions
     */
    private void initializeTables() {
        AmazonDynamoDB ddb = getDynamoDb();
        DynamoDB dynamoDB = new DynamoDB(ddb);
        smokersamplesTable = dynamoDB.getTable(SMOKERSAMPLES_TABLENAME);
        smokersessionsTable = dynamoDB.getTable(SMOKERSESSIONS_TABLENAME);
        smokerstateTable = dynamoDB.getTable(SMOKERSTATE_TABLENAME);
    }

    private JsonSmokerSession findSession(Predicate<? super JsonSmokerSession> predicate, Function<Long, Boolean> useLowSamples) {
        return listAllSessionsWithoutSamples()
                .stream()
                .filter(predicate)
                .findFirst()
                .map(s -> this.addSamplesToSession(s, useLowSamples))
                .orElse(null);
    }

    private List<JsonSmokerSession> listAllSessionsWithoutSamples() {
        return getScanResult(new ScanSpec(), smokersessionsTable, JsonSmokerSession::fromItemWithoutSamples, JsonSmokerSession::compareTo);
    }

    private JsonSmokerSession addSamplesToSession(JsonSmokerSession session, Function<Long, Boolean> useLowSamples) {
        long samplesCount = session.getSamplesCount();
        long sessionId = session.getId();
        Boolean lowSamples = useLowSamples.apply(samplesCount);
        List<JsonSample> samples = findSamples(sessionId, lowSamples);
        return session.toBuilder().samples(samples).build();
    }

    private List<JsonSample> findSamples(long sessionId, boolean lowSamples) {
        ScanSpec scanSpec = getSamplesScanSpec(sessionId, lowSamples);
        return getScanResult(scanSpec, smokersamplesTable, JsonSample::fromItem, JsonSample::compareTo);
    }

    private ScanSpec getSamplesScanSpec(long sessionId, boolean lowSamples) {
        return lowSamples ? getLowSamplesScanSpec(sessionId) : getHighSampleScanSpec(sessionId);
    }

    private ScanSpec getHighSampleScanSpec(long sessionId) {
        return new ScanSpec()
                .withFilterExpression("sessionId = :id")
                .withValueMap(new ValueMap()
                        .withNumber(":id", sessionId)
                );
    }

    private ScanSpec getLowSamplesScanSpec(long sessionId) {
        return new ScanSpec()
                .withFilterExpression("sessionId = :id AND newMinute = :newMinute")
                .withValueMap(new ValueMap()
                        .withNumber(":id", sessionId)
                        .withNumber(":newMinute", 1)
                );
    }

}
