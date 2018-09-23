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
import my.service.readstack.model.JsonSmokerSession;
import my.service.writestack.model.SmokerSession;

import java.util.*;
import java.util.stream.Collectors;

public class ReadService {
    public List<String> listsessions() {
        System.out.println("MyResource: listSessions");
        final AmazonDynamoDB ddb = getDynamoDb();
        List<SmokerSession> sessions = listAllSessions(ddb);
        return sessions.stream().map(session -> session.getSessionDateTime()).collect(Collectors.toList());
    }

    public JsonSmokerSession lastsession() {
        System.out.println("MyResource: listSession");
        final AmazonDynamoDB ddb = getDynamoDb();
        SmokerSession lastSession = findLastSession(ddb);
        boolean lowSamples = lastSession.getSamplesCount() > 360 * 6; // after 6 hour, use slower sample rate
        List<JsonSample> samples = findSamples(ddb, lastSession.getSessionStartTime(), lowSamples);
        return new JsonSmokerSession(lastSession, samples);
    }

    public JsonSmokerSession listSession(String session) {
        System.out.println("MyResource: session/" + session);
        final AmazonDynamoDB ddb = getDynamoDb();
        List<SmokerSession> sessions = findSession(ddb, session);
        if (sessions.isEmpty()) {
            return null;
        }
        SmokerSession smokerSession = sessions.get(0);
        boolean lowSamples = smokerSession.getSamplesCount() > 360 * 6; // after 6 hour, use slower sample rate
        List<JsonSample> samples = findSamples(ddb, smokerSession.getSessionStartTime(), lowSamples);
        return new JsonSmokerSession(smokerSession, samples);
    }


    private AmazonDynamoDB getDynamoDb() {
        return AmazonDynamoDBClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();
    }

    private List<JsonSample> findSamples(AmazonDynamoDB ddb, long sessionStartTime, boolean lowSamples) {
        DynamoDB dynamoDB = new DynamoDB(ddb);
        Table table = dynamoDB.getTable("smokersamples");
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
        ItemCollection<ScanOutcome> items = table.scan(scanSpec);
        try {
            List<JsonSample> result = new ArrayList<>();
            Iterator<Item> iter = items.iterator();
            while (iter.hasNext()) {
                Item item = iter.next();
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
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


    private SmokerSession findLastSession(AmazonDynamoDB ddb) {
        List<SmokerSession> scanResult = listAllSessions(ddb);
        OptionalLong lastSession = scanResult.stream().mapToLong(sessions -> sessions.getLastSampleTime()).max();
        long sessionDatetime = lastSession.orElseGet(() -> 0);
        Optional<SmokerSession> last = scanResult.stream().filter(session -> session.getLastSampleTime() == sessionDatetime).findFirst();
        return last.orElse(null);
    }

    private List<SmokerSession> listAllSessions(AmazonDynamoDB ddb) {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        return mapper.scan(SmokerSession.class, scanExpression);
    }

    private List<SmokerSession> findSession(AmazonDynamoDB ddb, String dateTimeString) {
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


}
