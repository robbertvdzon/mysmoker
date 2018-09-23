package my.service.writestack;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.*;
import my.service.writestack.model.Sample;
import my.service.writestack.model.SmokerSession;

import java.text.SimpleDateFormat;
import java.util.*;

public class WriteService {

    public SmokerSession newsession() {
        System.out.println("MyResource: newSession");
        final AmazonDynamoDB ddb = getDynamoDb();
        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateString = simpleDateFormat.format(new Date(currentTimeMillis));
        SmokerSession smokerSession = new SmokerSession(currentDateString, currentTimeMillis);
        smokerSession.setLastSampleTime(currentTimeMillis);
        storeSession(ddb, smokerSession);
        return smokerSession;
    }

    public void add(
            double bbqtemp,
            double meattemp,
            double bbqtempset,
            double fan
    ) {
        System.out.println("MyResource: add");
        try {
            final AmazonDynamoDB ddb = getDynamoDb();
            createTablewhenNeeded(ddb);

            long currentTimeMillis = System.currentTimeMillis();
            SmokerSession smokerSession = findOrCreateSession(ddb, currentTimeMillis);
            long timeDiffSinceLastMinute = currentTimeMillis - smokerSession.getLastMinuteSampleTime();
            boolean newMinute = timeDiffSinceLastMinute > 60 * 1000;

            Sample sample = new Sample();
            sample.setBbqTemp(bbqtemp);
            sample.setBbqSet(bbqtempset);
            sample.setFan(fan);
            sample.setMeatTemp(meattemp);
            sample.setNewMinute(newMinute);
            sample.setSessionStartTime(smokerSession.getSessionStartTime());
            sample.setTime(currentTimeMillis);

            if (newMinute) {
                smokerSession.setLastMinuteSampleTime(currentTimeMillis);
            }
            smokerSession.setLastBbqSet(sample.getBbqSet());
            smokerSession.setLastBbqTemp(sample.getBbqTemp());
            smokerSession.setLastMeatTemp(sample.getMeatTemp());
            smokerSession.setLastFan(sample.getFan());
            smokerSession.setLastSampleTime(currentTimeMillis);
            smokerSession.setSamplesCount(smokerSession.getSamplesCount() + 1);

            storeSession(ddb, smokerSession);
            storeSample(ddb, sample);
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
            System.err.format("Error: The table can't be found.\n");
            System.err.println("Be sure that it exists");
            System.exit(1);
        } catch (AmazonServiceException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println(t);
        }
    }

    private AmazonDynamoDB getDynamoDb() {
        return AmazonDynamoDBClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();
    }

    private void storeSession(AmazonDynamoDB ddb, SmokerSession smokerSession) {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        mapper.save(smokerSession);
    }

    private void storeSample(AmazonDynamoDB ddb, Sample sample) {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        mapper.save(sample);
    }


    private SmokerSession findOrCreateSession(AmazonDynamoDB ddb, long currentTimestamp) {
        long timeout = currentTimestamp - 1000 * 60 * 60;// minus one hour
        SmokerSession lastSession = findLastSession(ddb);
        if (lastSession != null && lastSession.getLastSampleTime() > timeout) {
            return lastSession;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateString = simpleDateFormat.format(new Date(currentTimestamp));
        return new SmokerSession(currentDateString, currentTimestamp);
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


    private void createTablewhenNeeded(AmazonDynamoDB dynamoDB) {
        createTable(dynamoDB, "smokersessions", "sessionDateTime", "S");
        createTable(dynamoDB, "smokersamples", "time", "N");
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


}
